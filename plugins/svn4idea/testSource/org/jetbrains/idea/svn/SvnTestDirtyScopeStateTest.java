/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.idea.svn;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FilePathImpl;
import com.intellij.openapi.vcs.ObjectsConvertor;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManagerImpl;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeVfsListener;
import com.intellij.openapi.vfs.VirtualFile;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SvnTestDirtyScopeStateTest extends Svn17TestCase {
  @Override
  public void setUp() throws Exception {
    myInitChangeListManager = false;
    super.setUp();

    final VcsDirtyScopeVfsListener vfsListener = ApplicationManager.getApplication().getComponent(VcsDirtyScopeVfsListener.class);
    vfsListener.setForbid(true);
  }

  @Test
  public void testWhatIsDirty() throws Exception {
    enableSilentOperation(VcsConfiguration.StandardConfirmation.ADD);

    final VcsDirtyScopeManagerImpl vcsDirtyScopeManager = (VcsDirtyScopeManagerImpl) VcsDirtyScopeManager.getInstance(myProject);

    final VirtualFile file = createFileInCommand("a.txt", "old content");
    final VirtualFile fileB = createFileInCommand("b.txt", "old content");
    final VirtualFile fileC = createFileInCommand("c.txt", "old content");
    final VirtualFile fileD = createFileInCommand("d.txt", "old content");
    waitABit();

    final List<FilePath> list = ObjectsConvertor.vf2fp(Arrays.asList(file, fileB, fileC, fileD));

    vcsDirtyScopeManager.retrieveScopes();
    vcsDirtyScopeManager.changesProcessed();

    vcsDirtyScopeManager.fileDirty(file);
    vcsDirtyScopeManager.fileDirty(fileB);

    final Collection<FilePath> dirty1 = vcsDirtyScopeManager.whatFilesDirty(list);
    Assert.assertTrue(dirty1.contains(new FilePathImpl(file)));
    Assert.assertTrue(dirty1.contains(new FilePathImpl(fileB)));

    Assert.assertTrue(! dirty1.contains(new FilePathImpl(fileC)));
    Assert.assertTrue(! dirty1.contains(new FilePathImpl(fileD)));

    vcsDirtyScopeManager.retrieveScopes();

    final Collection<FilePath> dirty2 = vcsDirtyScopeManager.whatFilesDirty(list);
    Assert.assertTrue(dirty2.contains(new FilePathImpl(file)));
    Assert.assertTrue(dirty2.contains(new FilePathImpl(fileB)));

    Assert.assertTrue(! dirty2.contains(new FilePathImpl(fileC)));
    Assert.assertTrue(! dirty2.contains(new FilePathImpl(fileD)));

    vcsDirtyScopeManager.changesProcessed();

    final Collection<FilePath> dirty3 = vcsDirtyScopeManager.whatFilesDirty(list);
    Assert.assertTrue(! dirty3.contains(new FilePathImpl(file)));
    Assert.assertTrue(! dirty3.contains(new FilePathImpl(fileB)));

    Assert.assertTrue(! dirty3.contains(new FilePathImpl(fileC)));
    Assert.assertTrue(! dirty3.contains(new FilePathImpl(fileD)));
  }

  private void waitABit() {
    try {
      Thread.sleep(100);
    }
    catch (InterruptedException e) {
      //
    }
  }

  @Test
  public void testOkToAddScopeUnderWriteAction() throws Exception {
    enableSilentOperation(VcsConfiguration.StandardConfirmation.ADD);

    final VcsDirtyScopeManagerImpl vcsDirtyScopeManager = (VcsDirtyScopeManagerImpl) VcsDirtyScopeManager.getInstance(myProject);

    final VirtualFile file = createFileInCommand("a.txt", "old content");
    final VirtualFile fileB = createFileInCommand("b.txt", "old content");
    final VirtualFile fileC = createFileInCommand("c.txt", "old content");
    final VirtualFile fileD = createFileInCommand("d.txt", "old content");
    waitABit();

    final List<FilePath> list = ObjectsConvertor.vf2fp(Arrays.asList(file, fileB, fileC, fileD));

    vcsDirtyScopeManager.retrieveScopes();
    vcsDirtyScopeManager.changesProcessed();

    new WriteCommandAction.Simple(myProject) {
      @Override
      protected void run() throws Throwable {
        vcsDirtyScopeManager.fileDirty(file);
        vcsDirtyScopeManager.fileDirty(fileB);
      }
    }.execute().throwException();


    final FilePathImpl fp = new FilePathImpl(file);
    final FilePathImpl fpB = new FilePathImpl(fileB);
    final long start = System.currentTimeMillis();
    while (System.currentTimeMillis() < (start + 3000)) {
      synchronized (this) {
        try {
          wait(50);
        }
        catch (InterruptedException e) {
          //
        }
      }
      final Collection<FilePath> dirty1 = vcsDirtyScopeManager.whatFilesDirty(list);
      if (dirty1.contains(fp) && dirty1.contains(fpB)) return;
    }
    Assert.assertTrue(false);
  }
}
