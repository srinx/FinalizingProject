package com.myProject.working.swing.standalone;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author sridhar
 */
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileSystemView;
import javax.imageio.ImageIO;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.nio.channels.FileChannel;

import java.net.URL;

class FileManager {

    public static final String APP_TITLE = "tree + view!! working finally :-))";
    private FileSystemView fileSystemView;
    private File currentFile;
    
    private JPanel gui;
    private Desktop desktop;
    private JTree tree;
    private DefaultTreeModel treeModel;
    private JTable table;
    private FileTableModel fileTableModel;
    private ListSelectionListener listSelectionListener;
    private boolean cellSizesSet = false;
    private int rowIconPadding = 6;
    private JPanel newFilePanel;
    private JRadioButton newTypeFile;
    private JTextField name;

    public Container getGui()
    {
        if (gui == null) {
            gui = new JPanel(new BorderLayout(1, 1));
            gui.setBorder(new EmptyBorder(5, 5, 5, 5));

            fileSystemView = FileSystemView.getFileSystemView();
            desktop = Desktop.getDesktop();

            JPanel detailView = new JPanel(new BorderLayout(10, 10));
            //fileTableModel = new FileTableModel();

            table = new JTable();
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoCreateRowSorter(true);
            table.setShowVerticalLines(false);
            table.setShowHorizontalLines(false);

            listSelectionListener = new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent lse)
                {
                    int row = table.getSelectionModel().getLeadSelectionIndex();
                    // setFileDetails( ((FileTableModel)table.getModel()).getFile(row) );
                }
            };
            table.getSelectionModel().addListSelectionListener(listSelectionListener);
            JScrollPane tableScroll = new JScrollPane(table);
            Dimension d = tableScroll.getPreferredSize();
            tableScroll.setPreferredSize(new Dimension((int) d.getWidth(), (int) d.getHeight() / 2));
            detailView.add(tableScroll, BorderLayout.CENTER);

            // the File tree
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            treeModel = new DefaultTreeModel(root);

            TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent tse)
                {
                    DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode) tse.getPath().getLastPathComponent();
                    showChildren(node);
                    //setFileDetails((File)node.getUserObject());
                }
            };

            // show the file system roots.
            File[] roots = fileSystemView.getRoots();
            for (File fileSystemRoot : roots) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
                root.add(node);
                //showChildren(node);
                //
                File[] files = fileSystemView.getFiles(fileSystemRoot, true);
                for (File file : files) {
                    if (file.isDirectory()) {
                        node.add(new DefaultMutableTreeNode(file));
                    }
                }
                //
            }

            tree = new JTree(treeModel);
            tree.setRootVisible(false);
            tree.addTreeSelectionListener(treeSelectionListener);
            tree.setCellRenderer(new FileTreeCellRenderer());
            tree.expandRow(0);
            JScrollPane treeScroll = new JScrollPane(tree);

            tree.setVisibleRowCount(15);

            Dimension preferredSize = treeScroll.getPreferredSize();
            Dimension widePreferred = new Dimension(
                    200,
                    (int) preferredSize.getHeight());
            treeScroll.setPreferredSize(widePreferred);

            // details for a File
            JPanel fileMainDetails = new JPanel(new BorderLayout(4, 2));
            fileMainDetails.setBorder(new EmptyBorder(0, 6, 0, 6));

            JPanel fileView = new JPanel(new BorderLayout(3, 3));

            fileView.add(fileMainDetails, BorderLayout.CENTER);

            detailView.add(fileView, BorderLayout.SOUTH);

            JSplitPane splitPane = new JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT,
                    treeScroll,
                    detailView);
            gui.add(splitPane, BorderLayout.CENTER);

        }
        return gui;
    }

    public void showRootFile()
    {
        tree.setSelectionInterval(0, 0);
    }

    private TreePath findTreePath(File find)
    {
        for (int ii = 0; ii < tree.getRowCount(); ii++) {
            TreePath treePath = tree.getPathForRow(ii);
            Object object = treePath.getLastPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
            File nodeFile = (File) node.getUserObject();

            if (nodeFile == find) {
                return treePath;
            }
        }

        return null;
    }

    private void showErrorMessage(String errorMessage, String errorTitle)
    {
        JOptionPane.showMessageDialog(
                gui,
                errorMessage,
                errorTitle,
                JOptionPane.ERROR_MESSAGE);
    }

    private void showThrowable(Throwable t)
    {
        t.printStackTrace();
        JOptionPane.showMessageDialog(
                gui,
                t.toString(),
                t.getMessage(),
                JOptionPane.ERROR_MESSAGE);
        gui.repaint();
    }

    private void setTableData(final File[] files)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                if (fileTableModel == null) {
                    fileTableModel = new FileTableModel();
                    table.setModel(fileTableModel);
                }
                table.getSelectionModel().removeListSelectionListener(listSelectionListener);
                fileTableModel.setFiles(files);
                table.getSelectionModel().addListSelectionListener(listSelectionListener);
                if (!cellSizesSet) {
                    Icon icon = fileSystemView.getSystemIcon(files[0]);

                    table.setRowHeight(icon.getIconHeight() + rowIconPadding);

                    setColumnWidth(0, -1);
                    setColumnWidth(3, 60);
                    table.getColumnModel().getColumn(3).setMaxWidth(120);
                    setColumnWidth(4, -1);
                    setColumnWidth(5, -1);
                    setColumnWidth(6, -1);
                    setColumnWidth(7, -1);
                    setColumnWidth(8, -1);
                    setColumnWidth(9, -1);

                    cellSizesSet = true;
                }
            }
        });
    }

    private void setColumnWidth(int column, int width)
    {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        if (width < 0) {

            JLabel label = new JLabel((String) tableColumn.getHeaderValue());
            Dimension preferred = label.getPreferredSize();

            width = (int) preferred.getWidth() + 14;
        }
        tableColumn.setPreferredWidth(width);
        tableColumn.setMaxWidth(width);
        tableColumn.setMinWidth(width);
    }

    private void showChildren(final DefaultMutableTreeNode node)
    {
        tree.setEnabled(false);

        SwingWorker<Void, File> worker = new SwingWorker<Void, File>() {
            public Void doInBackground()
            {
                File file = (File) node.getUserObject();
                if (file.isDirectory()) {
                    File[] files = fileSystemView.getFiles(file, true); //!!
                    if (node.isLeaf()) {
                        for (File child : files) {
                            if (child.isDirectory()) {
                                publish(child);
                            }
                        }
                    }
                    setTableData(files);
                }
                return null;
            }

            protected void process(List<File> chunks)
            {
                for (File child : chunks) {
                    node.add(new DefaultMutableTreeNode(child));
                }
            }

            protected void done()
            {
                tree.setEnabled(true);
            }
        };
        worker.execute();
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                try {

                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ITried) {
                }
                JFrame f = new JFrame(APP_TITLE);
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                FileManager fileManager = new FileManager();
                f.setContentPane(fileManager.getGui());                                
                f.pack();                                                         
                f.setLocationByPlatform(true);                
                f.setMinimumSize(f.getSize());                              
                f.setSize(900, 700);                                
                f.setVisible(true);

                fileManager.showRootFile();
            }
        });
    }
}

// A TableModel to hold File[]
class FileTableModel extends AbstractTableModel {

    private File[] files;
    private FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    private String[] columns = {"Type ", "File", "Path", "Size", "Last Modified", "R", "W", "E", "D", "F",};

    FileTableModel()
    {
        this(new File[0]);
    }

    FileTableModel(File[] files)
    {
        this.files = files;
    }

    public Object getValueAt(int row, int column)
    {
        File file = files[row];
        switch (column) {
            case 0:
                return fileSystemView.getSystemIcon(file);
            case 1:
                return fileSystemView.getSystemDisplayName(file);
            case 2:
                return file.getPath();
            case 3:
                return file.length();
            case 4:
                return file.lastModified();
            case 5:
                return file.canRead();
            case 6:
                return file.canWrite();
            case 7:
                return file.canExecute();
            case 8:
                return file.isDirectory();
            case 9:
                return file.isFile();
            default:
                System.err.println("Logic Error");
        }
        return "";
    }

    public int getColumnCount()
    {
        return columns.length;
    }

    public Class<?> getColumnClass(int column)
    {
        switch (column) {
            case 0:
                return ImageIcon.class;
            case 3:
                return Long.class;
            case 4:
                return Date.class;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                return Boolean.class;
        }
        return String.class;
    }

    public String getColumnName(int column)
    {
        return columns[column];
    }

    public int getRowCount()
    {
        return files.length;
    }

    public File getFile(int row)
    {
        return files[row];
    }

    public void setFiles(File[] files)
    {
        this.files = files;
        fireTableDataChanged();
    }
}

class FileTreeCellRenderer extends DefaultTreeCellRenderer {

    private FileSystemView fileSystemView;
    private JLabel label;

    FileTreeCellRenderer()
    {
        label = new JLabel();
        label.setOpaque(true);
        fileSystemView = FileSystemView.getFileSystemView();
    }

    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus)
    {

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        File file = (File) node.getUserObject();
        label.setIcon(fileSystemView.getSystemIcon(file));
        label.setText(fileSystemView.getSystemDisplayName(file));
        label.setToolTipText(file.getPath());

        if (selected) {
            label.setBackground(backgroundSelectionColor);
            label.setForeground(textSelectionColor);
        } else {
            label.setBackground(backgroundNonSelectionColor);
            label.setForeground(textNonSelectionColor);
        }

        return label;
    }
}
