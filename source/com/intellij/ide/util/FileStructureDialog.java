
package com.intellij.ide.util;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.commander.CommanderPanel;
import com.intellij.ide.commander.ProjectListBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.smartTree.SmartTreeStructure;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ex.IdeFocusTraversalPolicy;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.intellij.ui.ListScrollingUtil;
import com.intellij.ui.SpeedSearchBase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class FileStructureDialog extends DialogWrapper {
  private final Editor myEditor;
  private Navigatable myNavigatable;
  private final Project myProject;
  private MyCommanderPanel myCommanderPanel;
  private final StructureViewModel myTreeModel;
  private final Disposable myAuxDisposable;

  @NonNls private static final String ourPropertyKey = "FileStructure.narrowDown";
  private boolean myShouldNarrowDown = false;

  public FileStructureDialog(StructureViewModel structureViewModel, @Nullable Editor editor, Project project, Navigatable navigatable,
                             @NotNull final Disposable auxDisposable) {
    super(project, true);
    myProject = project;
    myEditor = editor;
    myNavigatable = navigatable;
    myTreeModel = structureViewModel;
    myAuxDisposable = auxDisposable;

    PsiFile psiFile = getPsiFile(project);

    final PsiElement psiElement = getCurrentElement(psiFile);

    //myDialog.setUndecorated(true);
    init();

    if (psiElement != null) {
      if (psiElement instanceof PsiClass) {
        myCommanderPanel.getBuilder().enterElement(psiElement, psiElement.getContainingFile().getVirtualFile());
      }
      else {
        myCommanderPanel.getBuilder().selectElement(psiElement, PsiUtil.getVirtualFile(psiElement));
      }
    }

    Disposer.register(myDisposable, myAuxDisposable);
  }

  protected PsiFile getPsiFile(final Project project) {
    return PsiDocumentManager.getInstance(project).getPsiFile(myEditor.getDocument());
  }

  protected Border createContentPaneBorder(){
    return null;
  }

  public void dispose() {
    myCommanderPanel.dispose();
    super.dispose();
  }

  protected String getDimensionServiceKey(){
    return "#com.intellij.ide.util.FileStructureDialog";
  }

  public JComponent getPreferredFocusedComponent() {
    return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myCommanderPanel);
  }

  @Nullable
  protected PsiElement getCurrentElement(@Nullable final PsiFile psiFile) {
    if (psiFile == null) return null;

    PsiDocumentManager.getInstance(myProject).commitAllDocuments();

    PsiElement element = psiFile.findElementAt(myEditor.getCaretModel().getOffset());
    for (; element != null; element = element.getParent()) {
      if (element instanceof PsiMethod) {
        PsiMethod method = (PsiMethod)element;
        PsiElement parent = method.getParent();
        if (parent instanceof PsiClass) {
          PsiClass psiClass = (PsiClass)parent;
          String name = psiClass.getQualifiedName();
          if (name == null) continue;
          return method;
        }
      }
      else if (element instanceof PsiField) {
        PsiField field = (PsiField)element;
        PsiElement parent = field.getParent();
        if (parent instanceof PsiClass) {
          PsiClass psiClass = (PsiClass)parent;
          String name = psiClass.getQualifiedName();
          if (name == null) continue;
          return field;
        }
      }
      else if (element instanceof PsiClass) {
        PsiClass psiClass = (PsiClass)element;
        String name = psiClass.getQualifiedName();
        if (name == null) continue;
        return psiClass;
      }
    }
    
    Object elementAtCursor = myTreeModel.getCurrentEditorElement();
    if (elementAtCursor instanceof PsiElement) {
      return (PsiElement) elementAtCursor;
    }

    return null;
  }

  protected JComponent createCenterPanel() {
    myCommanderPanel = new MyCommanderPanel(myProject);

    AbstractTreeStructure treeStructure = new MyStructureTreeStructure();

    PsiFile psiFile = getPsiFile(myProject);
    StructureViewBuilder viewBuilder = psiFile.getLanguage().getStructureViewBuilder(psiFile);
    boolean showRoot = viewBuilder instanceof TreeBasedStructureViewBuilder && ((TreeBasedStructureViewBuilder)viewBuilder).isRootNodeShown();
    ProjectListBuilder projectListBuilder = new ProjectListBuilder(myProject, myCommanderPanel, treeStructure, null, showRoot) {
      protected boolean nodeIsAcceptableForElement(AbstractTreeNode node, Object element) {
        return Comparing.equal(((StructureViewTreeElement)node.getValue()).getValue(), element);
      }

      protected void refreshSelection() {
        if (myShouldNarrowDown) {
          myCommanderPanel.updateSpeedSearch();
        }
      }

      protected List<AbstractTreeNode> getAllAcceptableNodes(final Object[] childElements, VirtualFile file) {
        ArrayList<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();
        for (Object childElement : childElements) {
          result.add((AbstractTreeNode)childElement);
        }
        return result;
      }
    };
    myCommanderPanel.setBuilder(projectListBuilder);
    myCommanderPanel.setTitlePanelVisible(false);

    new AnAction() {
      public void actionPerformed(AnActionEvent e) {
        final boolean succeeded = myCommanderPanel.navigateSelectedElement();
        if (succeeded) {
          unregisterCustomShortcutSet(myCommanderPanel);
        }
      }
    }.registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE).getShortcutSet(), myCommanderPanel);

    myCommanderPanel.setPreferredSize(new Dimension(400,500));

    JPanel panel = new JPanel(new GridBagLayout());
    final JCheckBox checkBox = new JCheckBox(IdeBundle.message("checkbox.narrow.down.the.list.on.typing"));
    checkBox.setSelected(PropertiesComponent.getInstance().isTrueValue(ourPropertyKey));
    checkBox.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e){
        myShouldNarrowDown = checkBox.isSelected();
        PropertiesComponent.getInstance().setValue(ourPropertyKey, Boolean.toString(myShouldNarrowDown));

        ProjectListBuilder builder = (ProjectListBuilder)myCommanderPanel.getBuilder();
        if (builder == null) {
          return;
        }
        builder.addUpdateRequest();
      }
    });

    checkBox.setFocusable(false);

    panel.add(checkBox, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0));
    panel.add(myCommanderPanel, new GridBagConstraints(0,1,1,1,1,1,GridBagConstraints.WEST,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0));

    return panel;
  }

  protected JComponent createSouthPanel() {
    return null;
  }

  public CommanderPanel getPanel() {
    return myCommanderPanel;
  }

  private class MyCommanderPanel extends CommanderPanel implements DataProvider {
    protected boolean shouldDrillDownOnEmptyElement(final Object value) {
      return false;
    }

    public MyCommanderPanel(Project _project) {
      super(_project, false);
      myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      myListSpeedSearch.addChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt){
          ProjectListBuilder builder = (ProjectListBuilder)getBuilder();
          if (builder == null) {
            return;
          }
          builder.addUpdateRequest(hasPrefixShortened(evt));
          ApplicationManager.getApplication().invokeLater(new Runnable() {
                  public void run(){
                    int index = myList.getSelectedIndex();
                    if (index != -1 && index < myList.getModel().getSize()) {
                      myList.clearSelection();
                      ListScrollingUtil.selectItem(myList, index);
                    }
                    else {
                      ListScrollingUtil.ensureSelectionExists(myList);
                    }
                  }
                });
        }
      });
      myListSpeedSearch.setComparator(createSpeedSearchComparator());
    }

    private boolean hasPrefixShortened(final PropertyChangeEvent evt) {
      return evt.getNewValue() != null && evt.getOldValue() != null &&
             ((String)evt.getNewValue()).length() < ((String)evt.getOldValue()).length();
    }

    public boolean navigateSelectedElement() {
      final boolean succeeded = super.navigateSelectedElement();
      if (succeeded) {
        close(CANCEL_EXIT_CODE);
      }
      return succeeded;
    }

    public Object getData(String dataId) {
      Object selectedElement = myCommanderPanel.getSelectedValue();

      if (selectedElement instanceof TreeElement) selectedElement = ((StructureViewTreeElement)selectedElement).getValue();

      if (DataConstants.NAVIGATABLE.equals(dataId)) {
        return selectedElement instanceof Navigatable ? selectedElement : myNavigatable;
      }
      return getDataImpl(dataId);
    }

    public String getEnteredPrefix(){
      return myListSpeedSearch.getEnteredPrefix();
    }

    public void updateSpeedSearch() {
      myListSpeedSearch.refreshSelection();
    }
  }

  private class MyStructureTreeStructure extends SmartTreeStructure {
    public MyStructureTreeStructure() {
      super(FileStructureDialog.this.myProject, myTreeModel);
    }

    public Object[] getChildElements(Object element){
      Object[] childElements = super.getChildElements(element);

      if (!myShouldNarrowDown) {
        return childElements;
      }

      String enteredPrefix = myCommanderPanel.getEnteredPrefix();
      if (enteredPrefix == null) {
        return childElements;
      }

      ArrayList<Object> filteredElements = new ArrayList<Object>(childElements.length);
      SpeedSearchBase.SpeedSearchComparator speedSearchComparator = createSpeedSearchComparator();

      for (Object child : childElements) {
        if (child instanceof AbstractTreeNode) {
          Object value = ((AbstractTreeNode)child).getValue();
          if (value instanceof TreeElement) {
            String name = ((TreeElement)value).getPresentation().getPresentableText();
            if (name == null) {
              continue;
            }
            if (!speedSearchComparator.doCompare(enteredPrefix, name)) {
              continue;
            }
          }
        }
        filteredElements.add(child);
      }
      return filteredElements.toArray(new Object[filteredElements.size()]);
    }

  }

  private static SpeedSearchBase.SpeedSearchComparator createSpeedSearchComparator() {
    return new SpeedSearchBase.SpeedSearchComparator() {
      public void translateCharacter(final StringBuilder buf, final char ch) {
        if (ch == '*') {
          buf.append(".*"); // overrides '*' handling to skip (,) in parameter lists
        }
        else {
          if (ch == ':') {
            buf.append(".*"); //    get:int should match any getter returning int
          }
          super.translateCharacter(buf, ch);
        }
      }
    };
  }
}
