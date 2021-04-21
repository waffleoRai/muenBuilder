package waffleoRai_mubuilderGUI;

import javax.swing.JFrame;
import java.awt.GridBagLayout;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;
import waffleoRai_mubuilder.MebAsset;
import waffleoRai_mubuilder.MebGroup;
import waffleoRai_mubuilder.MebPackage;
import waffleoRai_mubuilder.MebTypes;
import waffleoRai_mubuilder.MuenProgramFiles;
import waffleoRai_mubuilder.MuenProject;
import waffleoRai_mubuilderGUI.dialogs.NewProjDialog;
import waffleoRai_mubuilderGUI.nodes.MebAssetNode;
import waffleoRai_mubuilderGUI.nodes.MebGroupNode;
import waffleoRai_mubuilderGUI.nodes.MebNode;
import waffleoRai_mubuilderGUI.nodes.MebPackNode;
import waffleoRai_mubuilderGUI.nodes.MebRootNode;

import javax.swing.JLabel;

import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JCheckBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

public class MuBuilderMainForm extends JFrame{

	/*----- Constants -----*/
	
	private static final long serialVersionUID = -6222200380356176333L;
	
	public static final int MIN_WIDTH = 510;
	public static final int MIN_HEIGHT = 420;
	
	private static final int BVIEW_TAB_ALL = 0;
	private static final int BVIEW_TAB_GROUP = 1;
	private static final int BVIEW_TAB_PKG = 2;
	
	/*----- Instance Variables -----*/
	
	// -- Data
	private String proj_path;
	private MuenProject proj;
	private MebAsset now_asset; //Asset loaded into preview pane
	
	// -- GUI Mode
	private int browse_mode;
	
	// -- Project Info
	private JLabel lblPcode;
	private JLabel lblPtimestamp;
	private JLabel lblPver;
	
	// -- Browse Panel
	private JList<MebAssetNode> lstAssets;
	private JTree treeGroups;
	private JTree treePacks;
	
	// -- Asset Info
	private JTextField txtName;
	private JTextField txtExport;
	private JTextField txtSrc;
	private JComboBox<TypeListing> cbType;
	private JComboBox<GroupListing> cbGroup;
	private JComboBox<PkgListing> cbPkg;
	private JLabel lblInstance;
	private JCheckBox cbExport;
	private JScrollPane spAssetPrev;
	
	private JButton btnSaveName;
	private JButton btnBrowse;
	private JButton btnEdit;
	
	// --
	private IndefProgressDialog waitDialog;
	private AssetTypeHandler edit_callback;
	
	/*----- Internal Classes -----*/
	
	private static class TypeListing implements Comparable<TypeListing>{
		public int id;
		
		public TypeListing(int tid) {id = tid;}
		
		public String toString() {
			String str = MebTypes.getTypeString(id);
			if (str == null) str = "__";
			return str + " (" + Integer.toHexString(id) + ")";
		}
		
		public boolean equals(Object o) {
			if(o == null) return false;
			if(o == this) return true;
			if(!(o instanceof TypeListing)) return false;
			
			TypeListing other = (TypeListing)o;
			return this.id == other.id;
		}
		
		public int hashCode() {return id;}

		public int compareTo(TypeListing o) {
			if(o == null) return 1;
			return this.toString().compareTo(o.toString());
		}

	}
	
	private static class GroupListing implements Comparable<GroupListing>{
		public MebGroup g;
		
		public GroupListing(MebGroup grp) {g = grp;}
		
		public String toString() {
			return g.getGroupName() + String.format(" (%08x)", g.getGroupID());
		}
		
		public boolean equals(Object o) {
			if(o == null) return false;
			if(o == this) return true;
			if(!(o instanceof GroupListing)) return false;
			
			GroupListing other = (GroupListing)o;
			return this.g.getGroupID() == other.g.getGroupID();
		}
		
		public int hashCode() {return g.getGroupID();}

		public int compareTo(GroupListing o) {
			if(o == null) return 1;
			return this.toString().compareTo(o.toString());
		}

	}
	
	private static class PkgListing implements Comparable<PkgListing>{
		public MebPackage p;
		
		public PkgListing(MebPackage pkg) {p = pkg;}
		
		public String toString() {
			return p.getName();
		}
		
		public boolean equals(Object o) {
			if(o == null) return false;
			if(o == this) return true;
			if(!(o instanceof PkgListing)) return false;
			
			PkgListing other = (PkgListing)o;
			return p == other.p;
		}
		
		public int hashCode() {return p.hashCode();}

		public int compareTo(PkgListing o) {
			if(o == null) return 1;
			return this.toString().compareTo(o.toString());
		}

	}
	
	/*----- Init -----*/
	
	public MuBuilderMainForm() {
		initGUI();
		updateToProject(); //null project
		updateToAsset();
	}
	
	private void initGUI() {
		
		setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
		setPreferredSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
		
		setTitle("Mu Builder");
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		getContentPane().setLayout(gridBagLayout);
		
		JPanel pnlProjInfo = new JPanel();
		pnlProjInfo.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
		GridBagConstraints gbc_pnlProjInfo = new GridBagConstraints();
		gbc_pnlProjInfo.gridwidth = 2;
		gbc_pnlProjInfo.insets = new Insets(0, 0, 5, 0);
		gbc_pnlProjInfo.fill = GridBagConstraints.BOTH;
		gbc_pnlProjInfo.gridx = 0;
		gbc_pnlProjInfo.gridy = 0;
		getContentPane().add(pnlProjInfo, gbc_pnlProjInfo);
		GridBagLayout gbl_pnlProjInfo = new GridBagLayout();
		gbl_pnlProjInfo.columnWidths = new int[]{0, 0, 0, 0, 0};
		gbl_pnlProjInfo.rowHeights = new int[]{0, 0, 0};
		gbl_pnlProjInfo.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_pnlProjInfo.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		pnlProjInfo.setLayout(gbl_pnlProjInfo);
		
		lblPcode = new JLabel("muenPROJ");
		lblPcode.setFont(new Font("Courier New", Font.BOLD, 13));
		GridBagConstraints gbc_lblPcode = new GridBagConstraints();
		gbc_lblPcode.insets = new Insets(5, 10, 5, 5);
		gbc_lblPcode.gridx = 0;
		gbc_lblPcode.gridy = 0;
		pnlProjInfo.add(lblPcode, gbc_lblPcode);
		
		lblPtimestamp = new JLabel("Last Modified: April 1, 2021 15:02:00 CDT");
		lblPtimestamp.setFont(new Font("Courier New", Font.BOLD, 11));
		GridBagConstraints gbc_lblPtimestamp = new GridBagConstraints();
		gbc_lblPtimestamp.insets = new Insets(5, 5, 5, 5);
		gbc_lblPtimestamp.gridx = 2;
		gbc_lblPtimestamp.gridy = 0;
		pnlProjInfo.add(lblPtimestamp, gbc_lblPtimestamp);
		
		lblPver = new JLabel("v 0.0.0");
		lblPver.setFont(new Font("Courier New", Font.BOLD, 11));
		GridBagConstraints gbc_lblPver = new GridBagConstraints();
		gbc_lblPver.insets = new Insets(0, 5, 5, 5);
		gbc_lblPver.gridx = 0;
		gbc_lblPver.gridy = 1;
		pnlProjInfo.add(lblPver, gbc_lblPver);
		
		JTabbedPane tpBrowse = new JTabbedPane(JTabbedPane.TOP);
		GridBagConstraints gbc_tpBrowse = new GridBagConstraints();
		gbc_tpBrowse.insets = new Insets(0, 0, 0, 5);
		gbc_tpBrowse.fill = GridBagConstraints.BOTH;
		gbc_tpBrowse.gridx = 0;
		gbc_tpBrowse.gridy = 1;
		getContentPane().add(tpBrowse, gbc_tpBrowse);
		tpBrowse.setSelectedIndex(BVIEW_TAB_PKG);
		tpBrowse.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				onTabChange(tpBrowse.getSelectedIndex());
			}
		});
		
		JScrollPane spAllAssets = new JScrollPane();
		spAllAssets.setViewportBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		tpBrowse.addTab("All Assets", null, spAllAssets, null);
		
		lstAssets = new JList<MebAssetNode>();
		spAllAssets.setViewportView(lstAssets);
		spAllAssets.addMouseListener(new MouseAdapter() {
			private int last_left = -1;
			
			public void mousePressed(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
					onBrowsePane_rightclick();
				}
				else if(e.getButton() == MouseEvent.BUTTON1) {
					int sel = lstAssets.getSelectedIndex();
					if(sel == last_left && last_left >= 0) {
						last_left = -1;
						onBrowsePane_doubleclick();
					}
					else {
						last_left = sel;
					}
				}
			}
		});
		
		JScrollPane spGroups = new JScrollPane();
		spGroups.setViewportBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		tpBrowse.addTab("Groups", null, spGroups, null);
		
		treeGroups = new JTree();
		spGroups.setViewportView(treeGroups);
		spGroups.addMouseListener(new MouseAdapter() {
			private Object last_left = null;
			
			public void mousePressed(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
					onBrowsePane_rightclick();
				}
				else if(e.getButton() == MouseEvent.BUTTON1) {
					TreePath selpath = treeGroups.getSelectionPath();
					if(selpath == null) {
						last_left = null;
						return;
					}
					
					Object pend = selpath.getLastPathComponent();
					if(pend == null) {
						last_left = null;
						return;
					}
					if(last_left == pend) {
						last_left = null;
						onBrowsePane_doubleclick();
					}
					else {
						last_left = pend;
					}
				}
			}
		});
		
		JScrollPane spPacks = new JScrollPane();
		spPacks.setViewportBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		tpBrowse.addTab("Packages", null, spPacks, null);
		
		treePacks = new JTree();
		spPacks.setViewportView(treePacks);
		spPacks.addMouseListener(new MouseAdapter() {
			private Object last_left = null;
			public void mousePressed(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
					onBrowsePane_rightclick();
				}
				else if(e.getButton() == MouseEvent.BUTTON1) {
					TreePath selpath = treePacks.getSelectionPath();
					if(selpath == null) {
						last_left = null;
						return;
					}
					
					Object pend = selpath.getLastPathComponent();
					if(pend == null) {
						last_left = null;
						return;
					}
					if(last_left == pend) {
						last_left = null;
						onBrowsePane_doubleclick();
					}
					else {
						last_left = pend;
					}
				}
			}
		});

		JPanel pnlView = new JPanel();
		GridBagConstraints gbc_pnlView = new GridBagConstraints();
		gbc_pnlView.fill = GridBagConstraints.BOTH;
		gbc_pnlView.gridx = 1;
		gbc_pnlView.gridy = 1;
		getContentPane().add(pnlView, gbc_pnlView);
		GridBagLayout gbl_pnlView = new GridBagLayout();
		gbl_pnlView.columnWidths = new int[]{0, 0, 0, 0};
		gbl_pnlView.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 13, 0, 0};
		gbl_pnlView.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_pnlView.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
		pnlView.setLayout(gbl_pnlView);
		
		JLabel lblNewLabel = new JLabel("Name:");
		lblNewLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel.insets = new Insets(5, 0, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		pnlView.add(lblNewLabel, gbc_lblNewLabel);
		
		txtName = new JTextField();
		GridBagConstraints gbc_txtName = new GridBagConstraints();
		gbc_txtName.insets = new Insets(5, 0, 5, 5);
		gbc_txtName.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtName.gridx = 1;
		gbc_txtName.gridy = 0;
		pnlView.add(txtName, gbc_txtName);
		txtName.setColumns(10);
		
		btnSaveName = new JButton("Save");
		GridBagConstraints gbc_btnSaveName = new GridBagConstraints();
		gbc_btnSaveName.insets = new Insets(5, 0, 5, 0);
		gbc_btnSaveName.gridx = 2;
		gbc_btnSaveName.gridy = 0;
		pnlView.add(btnSaveName, gbc_btnSaveName);
		btnSaveName.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onAssetPane_namesave();
			}	
		});
		
		JLabel lblTypeId = new JLabel("Type ID:");
		lblTypeId.setHorizontalAlignment(SwingConstants.RIGHT);
		lblTypeId.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblTypeId = new GridBagConstraints();
		gbc_lblTypeId.anchor = GridBagConstraints.EAST;
		gbc_lblTypeId.insets = new Insets(0, 0, 5, 5);
		gbc_lblTypeId.gridx = 0;
		gbc_lblTypeId.gridy = 1;
		pnlView.add(lblTypeId, gbc_lblTypeId);
		
		cbType = new JComboBox<TypeListing>();
		GridBagConstraints gbc_cbType = new GridBagConstraints();
		gbc_cbType.gridwidth = 2;
		gbc_cbType.insets = new Insets(0, 0, 5, 0);
		gbc_cbType.fill = GridBagConstraints.HORIZONTAL;
		gbc_cbType.gridx = 1;
		gbc_cbType.gridy = 1;
		pnlView.add(cbType, gbc_cbType);
		cbType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onAssetPane_setType();
			}	
		});
		
		JLabel lblGroupId = new JLabel("Group ID:");
		lblGroupId.setHorizontalAlignment(SwingConstants.RIGHT);
		lblGroupId.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblGroupId = new GridBagConstraints();
		gbc_lblGroupId.anchor = GridBagConstraints.EAST;
		gbc_lblGroupId.insets = new Insets(0, 0, 5, 5);
		gbc_lblGroupId.gridx = 0;
		gbc_lblGroupId.gridy = 2;
		pnlView.add(lblGroupId, gbc_lblGroupId);
		
		cbGroup = new JComboBox<GroupListing>();
		GridBagConstraints gbc_cbGroup = new GridBagConstraints();
		gbc_cbGroup.gridwidth = 2;
		gbc_cbGroup.insets = new Insets(0, 0, 5, 0);
		gbc_cbGroup.fill = GridBagConstraints.HORIZONTAL;
		gbc_cbGroup.gridx = 1;
		gbc_cbGroup.gridy = 2;
		pnlView.add(cbGroup, gbc_cbGroup);
		cbGroup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onAssetPane_setGroup();
			}	
		});
		
		JLabel lblInstanceId = new JLabel("Instance ID:");
		lblInstanceId.setHorizontalAlignment(SwingConstants.RIGHT);
		lblInstanceId.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblInstanceId = new GridBagConstraints();
		gbc_lblInstanceId.anchor = GridBagConstraints.EAST;
		gbc_lblInstanceId.insets = new Insets(0, 5, 5, 5);
		gbc_lblInstanceId.gridx = 0;
		gbc_lblInstanceId.gridy = 3;
		pnlView.add(lblInstanceId, gbc_lblInstanceId);
		
		lblInstance = new JLabel("0000000000000000");
		lblInstance.setFont(new Font("Tahoma", Font.PLAIN, 12));
		GridBagConstraints gbc_lblInstance = new GridBagConstraints();
		gbc_lblInstance.gridwidth = 2;
		gbc_lblInstance.insets = new Insets(0, 0, 5, 0);
		gbc_lblInstance.gridx = 1;
		gbc_lblInstance.gridy = 3;
		pnlView.add(lblInstance, gbc_lblInstance);
		
		JLabel lblExport = new JLabel("Export:");
		lblExport.setHorizontalAlignment(SwingConstants.RIGHT);
		lblExport.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblExport = new GridBagConstraints();
		gbc_lblExport.anchor = GridBagConstraints.EAST;
		gbc_lblExport.insets = new Insets(0, 0, 5, 5);
		gbc_lblExport.gridx = 0;
		gbc_lblExport.gridy = 4;
		pnlView.add(lblExport, gbc_lblExport);
		
		txtExport = new JTextField();
		GridBagConstraints gbc_txtExport = new GridBagConstraints();
		gbc_txtExport.insets = new Insets(0, 0, 5, 5);
		gbc_txtExport.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtExport.gridx = 1;
		gbc_txtExport.gridy = 4;
		pnlView.add(txtExport, gbc_txtExport);
		txtExport.setColumns(10);
		
		cbExport = new JCheckBox("");
		GridBagConstraints gbc_cbExport = new GridBagConstraints();
		gbc_cbExport.insets = new Insets(0, 0, 5, 0);
		gbc_cbExport.gridx = 2;
		gbc_cbExport.gridy = 4;
		pnlView.add(cbExport, gbc_cbExport);
		cbExport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onAssetPane_toggleExport();
			}	
		});
		
		JLabel lblSource = new JLabel("Source:");
		lblSource.setHorizontalAlignment(SwingConstants.RIGHT);
		lblSource.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblSource = new GridBagConstraints();
		gbc_lblSource.anchor = GridBagConstraints.EAST;
		gbc_lblSource.insets = new Insets(0, 0, 5, 5);
		gbc_lblSource.gridx = 0;
		gbc_lblSource.gridy = 5;
		pnlView.add(lblSource, gbc_lblSource);
		
		txtSrc = new JTextField();
		txtSrc.setColumns(10);
		GridBagConstraints gbc_txtSrc = new GridBagConstraints();
		gbc_txtSrc.gridwidth = 2;
		gbc_txtSrc.insets = new Insets(0, 0, 5, 0);
		gbc_txtSrc.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtSrc.gridx = 1;
		gbc_txtSrc.gridy = 5;
		pnlView.add(txtSrc, gbc_txtSrc);
		
		btnBrowse = new JButton("Browse...");
		GridBagConstraints gbc_btnBrowse = new GridBagConstraints();
		gbc_btnBrowse.insets = new Insets(0, 0, 5, 0);
		gbc_btnBrowse.gridx = 2;
		gbc_btnBrowse.gridy = 6;
		pnlView.add(btnBrowse, gbc_btnBrowse);
		btnBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onAssetPane_browseSource();
			}	
		});
		
		JLabel lblPackage = new JLabel("Package:");
		lblPackage.setHorizontalAlignment(SwingConstants.RIGHT);
		lblPackage.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblPackage = new GridBagConstraints();
		gbc_lblPackage.anchor = GridBagConstraints.EAST;
		gbc_lblPackage.insets = new Insets(0, 0, 10, 5);
		gbc_lblPackage.gridx = 0;
		gbc_lblPackage.gridy = 7;
		pnlView.add(lblPackage, gbc_lblPackage);
		
		cbPkg = new JComboBox<PkgListing>();
		GridBagConstraints gbc_cbPkg = new GridBagConstraints();
		gbc_cbPkg.gridwidth = 2;
		gbc_cbPkg.insets = new Insets(0, 0, 10, 0);
		gbc_cbPkg.fill = GridBagConstraints.HORIZONTAL;
		gbc_cbPkg.gridx = 1;
		gbc_cbPkg.gridy = 7;
		pnlView.add(cbPkg, gbc_cbPkg);
		cbPkg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onAssetPane_setPackage();
			}	
		});
		
		spAssetPrev = new JScrollPane();
		spAssetPrev.setViewportBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		GridBagConstraints gbc_spAssetPrev = new GridBagConstraints();
		gbc_spAssetPrev.insets = new Insets(0, 0, 5, 0);
		gbc_spAssetPrev.gridwidth = 3;
		gbc_spAssetPrev.fill = GridBagConstraints.BOTH;
		gbc_spAssetPrev.gridx = 0;
		gbc_spAssetPrev.gridy = 8;
		pnlView.add(spAssetPrev, gbc_spAssetPrev);
		
		btnEdit = new JButton("Edit...");
		GridBagConstraints gbc_btnEdit = new GridBagConstraints();
		gbc_btnEdit.insets = new Insets(0, 0, 5, 0);
		gbc_btnEdit.gridx = 2;
		gbc_btnEdit.gridy = 9;
		pnlView.add(btnEdit, gbc_btnEdit);
		btnEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onAssetPane_editAsset();
			}	
		});
		btnEdit.setEnabled(false);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem miNew = new JMenuItem("New Project...");
		mnFile.add(miNew);
		miNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onFile_new();
			}	
		});
		
		JMenuItem miOpen = new JMenuItem("Open...");
		mnFile.add(miOpen);
		miOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onFile_open();
			}	
		});
		
		JMenuItem miSave = new JMenuItem("Save");
		mnFile.add(miSave);
		miSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onFile_save();
			}	
		});
		
		JMenuItem miSaveas = new JMenuItem("Save As...");
		mnFile.add(miSaveas);
		miSaveas.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onFile_saveas();
			}	
		});
		
		JMenu mnProj = new JMenu("Project");
		menuBar.add(mnProj);
		
		JMenuItem miProjInfo = new JMenuItem("Project Info");
		mnProj.add(miProjInfo);
		miProjInfo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onProject_info();
			}	
		});
		
		JMenuItem miPacks = new JMenuItem("Packages...");
		mnProj.add(miPacks);
		miPacks.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onProject_packages();
			}	
		});
		
		JMenuItem miGroups = new JMenuItem("Groups...");
		mnProj.add(miGroups);
		miGroups.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onProject_groups();
			}	
		});
		
		JSeparator separator = new JSeparator();
		mnProj.add(separator);
		
		JMenuItem miImport = new JMenuItem("Import...");
		mnProj.add(miImport);
		miImport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onProject_import();
			}	
		});
		
		JSeparator separator_1 = new JSeparator();
		mnProj.add(separator_1);
		
		JMenuItem miBuild = new JMenuItem("Build");
		mnProj.add(miBuild);
		miBuild.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onProject_build();
			}	
		});
		
		JMenu mnEditor = new JMenu("Editor");
		menuBar.add(mnEditor);
		
		JMenu mnEditLayout = new JMenu("New Layout...");
		mnEditor.add(mnEditLayout);
		
		JMenuItem miLYO = new JMenuItem("UI Layout");
		mnEditLayout.add(miLYO);
		miLYO.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onEditor_LYO();
			}	
		});
		
		JMenuItem miTXB = new JMenuItem("Textbox");
		mnEditLayout.add(miTXB);
		miTXB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onEditor_TXB();
			}	
		});
		
		JMenuItem miANM = new JMenuItem("New Sprite Animation");
		mnEditor.add(miANM);
		miANM.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onEditor_ANM();
			}	
		});
		
		JMenuItem miTLM = new JMenuItem("New Tile Map");
		mnEditor.add(miTLM);
		miTLM.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onEditor_TLM();
			}	
		});
		
		JMenuItem miSCE = new JMenuItem("New Scene Script");
		mnEditor.add(miSCE);
		miSCE.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onEditor_SCE();
			}	
		});
	}
	
	/*----- GUI Updating -----*/
	
	public void setWait() {
		waitDialog = new IndefProgressDialog(this, "Processing");
		waitDialog.setPrimaryString("Please Wait");
		waitDialog.setSecondaryString("Running background process");
		
		waitDialog.setVisible(true);
	}
	
	public void unsetWait() {
		if(waitDialog == null) return;
		waitDialog.setVisible(false);
		waitDialog.dispose();
	}
	
	protected void updateToProject() {
		//Updates project info fields
		
		if(proj == null) {
			lblPcode.setText("(No project loaded)");
			lblPver.setText("--");
			lblPtimestamp.setText("--");
		}
		else {
			//Info panel
			lblPcode.setText(proj.getGamecodeString());
			lblPver.setText("v " + proj.getVersionString());
			ZonedDateTime timestamp = proj.getTimestamp();
			lblPtimestamp.setText("Modified: " + timestamp.format(DateTimeFormatter.RFC_1123_DATE_TIME));
		}
		
		lblPcode.repaint();
		lblPver.repaint();
		lblPtimestamp.repaint();
		updateBrowsePane();
		populateComboBoxes();
	}
	
	protected void updateToAsset() {
		
		if(waitDialog != null) {
			waitDialog.setPrimaryString("Loading asset");
			waitDialog.setSecondaryString("Updating asset information panel");
		}
		
		//Updates asset info fields
		boolean isnull = now_asset == null;
		txtName.setEnabled(!isnull);
		btnSaveName.setEnabled(!isnull);
		cbType.setEnabled(!isnull);
		cbGroup.setEnabled(!isnull);
		txtExport.setEnabled(!isnull);
		cbExport.setEnabled(!isnull);
		txtSrc.setEnabled(!isnull);
		btnBrowse.setEnabled(!isnull);
		cbPkg.setEnabled(!isnull);
		
		edit_callback = null;
		
		//if(btnEdit_listener != null) btnEdit.removeActionListener(btnEdit_listener);
		
		if(isnull) {
			txtName.setText("");
			txtExport.setText("");
			txtSrc.setText("");
			
			cbType.setSelectedIndex(-1);
			cbGroup.setSelectedIndex(-1);
			cbPkg.setSelectedIndex(-1);
			
			cbExport.setSelected(false);
			
			lblInstance.setText("----------------");
			spAssetPrev.setViewportView(null);
			btnEdit.setEnabled(false);
		}
		else {
			txtName.setText(now_asset.getLocalName());
			setTypeCmbx(now_asset.getTypeID());
			setGroupCmbx(now_asset.getGroupID());
			lblInstance.setText(String.format("%016x", now_asset.getInstanceID()));
			
			String ename = now_asset.getExportName();
			if(ename.isEmpty()) {
				ename = null;
				now_asset.setExportName(null);
			}
			if(ename == null) {
				cbExport.setSelected(false);
				txtExport.setText("");
				txtExport.setEnabled(false);
			}
			else {
				cbExport.setSelected(true);
				txtExport.setText(ename);
			}
			
			txtSrc.setText(now_asset.getSourcePath());
			//cbPkg.setSelectedIndex(now_asset.getPackageIndex());
			setPackageCmbx(now_asset.getPackageIndex());
			
			//Preview
			AssetTypeHandler thandle = AssetTypeHandler.getHandler(now_asset.getTypeID());
			if(thandle == null) {
				spAssetPrev.setViewportView(null);
				btnEdit.setEnabled(false);
			}
			else {
				if(waitDialog != null) {
					waitDialog.setSecondaryString("Generating preview");
				}
				
				edit_callback = thandle;
				btnEdit.setEnabled(thandle.isEditable());
				spAssetPrev.setViewportView(thandle.genPreviewPanel(now_asset.getSourcePath()));
			}
		}
		
		//Repaint
		txtName.repaint();
		txtExport.repaint();
		txtSrc.repaint();
		btnSaveName.repaint();
		btnBrowse.repaint();
		btnEdit.repaint();
		
		cbType.repaint();
		cbGroup.repaint();
		cbPkg.repaint();
		
		cbExport.repaint();
		
		lblInstance.repaint();
		spAssetPrev.repaint();
		
	}
	
	protected void updateBrowsePane() {
		//TODO also set selection to what was selected before?
		
		//Clear all
		lstAssets.setModel(new DefaultListModel<MebAssetNode>());
		treeGroups.setModel(new DefaultTreeModel(null));
		treePacks.setModel(new DefaultTreeModel(null));
		
		lstAssets.repaint();
		treeGroups.repaint();
		treePacks.repaint();
		
		if(proj == null) return;
		
		//Reload the current view
		MebAsset.sort_by_name = true;
		switch(browse_mode) {
		case BVIEW_TAB_ALL:
			List<MebAsset> alist = proj.getAllAssets();
			DefaultListModel<MebAsset> mdl = new DefaultListModel<MebAsset>();
			for(MebAsset a : alist)mdl.addElement(a);
			break;
		case BVIEW_TAB_GROUP:
			MebNode groot = generateGroupNodes();
			treeGroups.setModel(new DefaultTreeModel(groot));
			break;
		case BVIEW_TAB_PKG:
			MebNode proot = generatePackageNodes();
			treeGroups.setModel(new DefaultTreeModel(proot));
			break;
		}
		
	}
	
	protected void onTabChange(int t) {
		browse_mode = t;
		
		IndefProgressDialog dialog = new IndefProgressDialog(this, "Refreshing");
		dialog.setPrimaryString("Updating View");
		dialog.setSecondaryString("Loading browser view");
		
		SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){

			protected Void doInBackground() throws Exception {
				try{
					updateBrowsePane();
				}
				catch(Exception x){
					x.printStackTrace();
					showError("Unknown Error: Refresh Failed! See stderr for details.");
				}
				
				return null;
			}
			
			public void done()	{
				dialog.closeMe();
			}
		};
		
		task.execute();
		dialog.render();
		
	}
	
	protected void populateComboBoxes() {
		//Type box
		List<Integer> alltypes = MebTypes.getAllTypes();
		List<TypeListing> tlist = new ArrayList<TypeListing>(alltypes.size());
		for(Integer t:alltypes) {
			TypeListing tl = new TypeListing(t);
			tlist.add(tl);
		}
		Collections.sort(tlist);
		
		DefaultComboBoxModel<TypeListing> mdl = new DefaultComboBoxModel<TypeListing>();
		for(TypeListing tl : tlist) mdl.addElement(tl);
		
		cbType.setModel(mdl);
		cbType.repaint();
		
		if(proj == null) {
			cbGroup.setModel(new DefaultComboBoxModel<GroupListing>());
			cbPkg.setModel(new DefaultComboBoxModel<PkgListing>());
			cbGroup.repaint();
			cbPkg.repaint();
			return;
		}
		
		//Group box
		List<MebGroup> grplist = proj.getAllGroups();
		List<GroupListing> glist = new ArrayList<GroupListing>(grplist.size());
		for(MebGroup g : grplist) {
			glist.add(new GroupListing(g));
		}
		Collections.sort(glist);
		
		DefaultComboBoxModel<GroupListing> mdlg = new DefaultComboBoxModel<GroupListing>();
		for(GroupListing gl : glist) mdlg.addElement(gl);
		
		cbGroup.setModel(mdlg);
		cbGroup.repaint();
		
		//Package box
		List<MebPackage> pkglist = proj.getAllPackages();
		List<PkgListing> plist = new ArrayList<PkgListing>(pkglist.size());
		for(MebPackage p : pkglist) {
			plist.add(new PkgListing(p));
		}
		Collections.sort(plist);
		
		DefaultComboBoxModel<PkgListing> mdlp = new DefaultComboBoxModel<PkgListing>();
		for(PkgListing pl : plist) mdlp.addElement(pl);
		
		cbPkg.setModel(mdlp);
		cbPkg.repaint();
	}
	
	protected void setTypeCmbx(int typeID) {
		int icount = cbType.getItemCount();
		for(int i = 0; i < icount; i++) {
			TypeListing tl = cbType.getItemAt(i);
			if(tl.id == typeID) {
				cbType.setSelectedItem(i);
				return;
			}
		}
		cbType.setSelectedIndex(-1);
		cbType.repaint();
	}
	
	protected void setGroupCmbx(int groupID) {
		int icount = cbGroup.getItemCount();
		for(int i = 0; i < icount; i++) {
			GroupListing gl = cbGroup.getItemAt(i);
			if(gl.g.getGroupID() == groupID) {
				cbGroup.setSelectedItem(i);
				return;
			}
		}
		cbGroup.setSelectedIndex(-1);
		cbGroup.repaint();
	}
	
	protected void setPackageCmbx(int pkg_idx) {
		int icount = cbPkg.getItemCount();
		for(int i = 0; i < icount; i++) {
			PkgListing pl = cbPkg.getItemAt(i);
			if(pl.p.getIndex() == pkg_idx) {
				cbPkg.setSelectedItem(i);
				return;
			}
		}
		cbPkg.setSelectedIndex(-1);
		cbPkg.repaint();
	}

	protected MebNode generateGroupNodes() {
		if(this.proj == null) return null;
		List<MebGroup> glist = proj.getAllGroups();
		MebRootNode root = new MebRootNode(proj.getGamecodeString());
		for(MebGroup g : glist) {
			MebGroupNode gn = new MebGroupNode(g);
			gn.setParent(root);
		}
		return root;
	}
	
	protected MebNode generatePackageNodes() {
		if(this.proj == null) return null;
		List<MebPackage> plist = proj.getAllPackages();
		MebRootNode root = new MebRootNode(proj.getGamecodeString());
		for(MebPackage p : plist) {
			MebPackNode pn = new MebPackNode(p);
			pn.setParent(root);
		}
		return root;
	}
	
	/*----- State -----*/
	
	protected List<MebAsset> getSelectedAssets(){
		List<MebAsset> outlist = new LinkedList<MebAsset>();
		int c = 0;
		switch(browse_mode) {
		case BVIEW_TAB_ALL:
			List<MebAssetNode> sellist = lstAssets.getSelectedValuesList();
			if(sellist == null) return outlist;
			for(MebAssetNode n : sellist) {
				outlist.add(n.asset);
			}
			return outlist;
		case BVIEW_TAB_GROUP:
			TreePath[] gpaths = treeGroups.getSelectionPaths();
			c = gpaths.length;
			for(int i = 0; i < c; i++) {
				TreePath p = gpaths[i];
				Object o = p.getLastPathComponent();
				if(o instanceof MebAssetNode) {
					outlist.add(((MebAssetNode)o).asset);
				}
			}
			break;
		case BVIEW_TAB_PKG:
			TreePath[] ppaths = treePacks.getSelectionPaths();
			c = ppaths.length;
			for(int i = 0; i < c; i++) {
				TreePath p = ppaths[i];
				Object o = p.getLastPathComponent();
				if(o instanceof MebAssetNode) {
					outlist.add(((MebAssetNode)o).asset);
				}
			}
			break;
		}
		
		return outlist;
	}
	
	protected List<MebGroup> getSelectedGroups(){
		List<MebGroup> outlist = new LinkedList<MebGroup>();
		if(browse_mode == BVIEW_TAB_GROUP) {
			TreePath[] gpaths = treeGroups.getSelectionPaths();
			int c = gpaths.length;
			for(int i = 0; i < c; i++) {
				TreePath p = gpaths[i];
				Object o = p.getLastPathComponent();
				if(o instanceof MebGroupNode) {
					outlist.add(((MebGroupNode)o).getGroup());
				}
			}
		}
		return outlist;
	}
	
	protected List<MebPackage> getSelectedPacks(){
		List<MebPackage> outlist = new LinkedList<MebPackage>();
		if(browse_mode == BVIEW_TAB_PKG) {
			TreePath[] paths = treeGroups.getSelectionPaths();
			int c = paths.length;
			for(int i = 0; i < c; i++) {
				TreePath p = paths[i];
				Object o = p.getLastPathComponent();
				if(o instanceof MebPackNode) {
					outlist.add(((MebPackNode)o).getPackage());
				}
			}
		}
		return outlist;
	}
	
	/*----- Actions -----*/
	
	//	-- Browse Pane
		
	protected void onBrowsePane_rightclick() {
		//Open up selection menu
		// RN just move, delete, view
		JPopupMenu menu = new JPopupMenu();
		
		JMenuItem itm = new JMenuItem("View");
		menu.add(itm);
		itm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onBrowsePane_viewAsset();
			}
		});
		
		itm = new JMenuItem("Move");
		menu.add(itm);
		itm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onBrowsePane_move();
			}
		});
		
		itm = new JMenuItem("Delete");
		itm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onBrowsePane_delete();
			}
		});
		
	}
	
	protected void onBrowsePane_move() {
		//TODO
		//Move selected assets to new group or package
		System.err.println("Unimplemented GUI command: onBrowsePane_move()");
		//Needs a dialog, don't feel like writing rn
	}
	
	protected void onBrowsePane_delete() {
		//Delete selected assets from project (after confirmation)
		int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete all selected elements from this project?\n"
				+ "This will include all selected assets, groups, and packages.\n"
				+ "Unselected assets within selected groups or packages will be moved to default group or package.", 
				"Delete Elements", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		
		if(confirm != JOptionPane.YES_OPTION) return;
		
		waitDialog = new IndefProgressDialog(this, "Deleting Elements");
		waitDialog.setPrimaryString("Please Wait");
		waitDialog.setSecondaryString("Removing selected elements from project");
		
		SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){

			protected Void doInBackground() throws Exception {
				if(proj == null) return null;
				try{
					//Assets
					List<MebAsset> alist = getSelectedAssets();
					for(MebAsset a : alist) proj.removeAsset(a.getInstanceID());
					
					List<MebGroup> glist = getSelectedGroups();
					for(MebGroup g : glist) proj.removeGroup(g.getGroupID());
					
					List<MebPackage> plist = getSelectedPacks();
					for(MebPackage p : plist) proj.removePackage(p.getIndex());
					
				}
				catch(Exception x){
					x.printStackTrace();
					showError("Unknown Error: Refresh Failed! See stderr for details.");
				}
				
				return null;
			}
			
			public void done()	{
				waitDialog.closeMe();
				waitDialog = null;
			}
		};
		
		task.execute();
		waitDialog.render();
	}
	
	protected void onBrowsePane_viewAsset() {
		onBrowsePane_doubleclick();
	}
	
	protected void onBrowsePane_doubleclick() {
		//If top node is an asset, load into panel.
		MebAssetNode snode = null;
		switch(browse_mode) {
		case BVIEW_TAB_ALL:
			snode = lstAssets.getSelectedValue();
			break;
		case BVIEW_TAB_GROUP:
			TreePath gpath = treeGroups.getSelectionPath();
			if(gpath != null) {
				Object s = gpath.getLastPathComponent();
				if(s instanceof MebAssetNode) {
					snode = (MebAssetNode)s;
				}
			}
			break;
		case BVIEW_TAB_PKG:
			TreePath ppath = treePacks.getSelectionPath();
			if(ppath != null) {
				Object s = ppath.getLastPathComponent();
				if(s instanceof MebAssetNode) {
					snode = (MebAssetNode)s;
				}
			}
			break;
		}
	
		if(snode == null) now_asset = null;
		else now_asset = snode.asset;
		
		waitDialog = new IndefProgressDialog(this, "Refreshing");
		
		SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){

			protected Void doInBackground() throws Exception {
				try{
					updateToAsset();
				}
				catch(Exception x){
					x.printStackTrace();
					showError("Unknown Error: Refresh Failed! See stderr for details.");
				}
				
				return null;
			}
			
			public void done()	{
				waitDialog.closeMe();
				waitDialog = null;
			}
		};
		
		task.execute();
		waitDialog.render();
		
	}
	
	//	-- Asset Info Pane
	
	protected void onAssetPane_namesave() {
		//Update visible asset name (and export name, if applicable)
		if(now_asset == null) return;	
		now_asset.setLocalName(txtName.getText());

		if(cbExport.isSelected()) {
			now_asset.setExportName(txtExport.getText());
		}
		proj.rebuildNameMap();	
		
		updateBrowsePane();
	}
	
	protected void onAssetPane_setType() {
		if(now_asset == null) return;	
		//Determine what type is selected...
		TypeListing tl = cbType.getItemAt(cbType.getSelectedIndex());
		if(tl == null) {
			//Reset the type box and return.
			setTypeCmbx(now_asset.getTypeID());
			return;
		}
		
		now_asset.setTypeID(tl.id);
		
		waitDialog = new IndefProgressDialog(this, "Refreshing");
		
		SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){

			protected Void doInBackground() throws Exception {
				try{
					//Update GUI (with preview if there is one)
					updateToAsset();
				}
				catch(Exception x){
					x.printStackTrace();
					showError("Unknown Error: Refresh Failed! See stderr for details.");
				}
				
				return null;
			}
			
			public void done()	{
				waitDialog.closeMe();
				waitDialog = null;
			}
		};
		
		task.execute();
		waitDialog.render();
		
	}
	
	protected void onAssetPane_setGroup() {
		if(now_asset == null) return;	
		GroupListing gl = cbGroup.getItemAt(cbGroup.getSelectedIndex());
		if(gl == null) {
			//Reset the box and return.
			setGroupCmbx(now_asset.getGroupID());
			return;
		}
		
		MebGroup g = gl.g;
		if(g == null) return;
		g.addAsset(now_asset);
		
		//If group view, refresh browse pane.
		if(browse_mode == BVIEW_TAB_GROUP) updateBrowsePane();
	}
	
	protected void onAssetPane_setPackage() {
		if(now_asset == null) return;
		PkgListing pl = cbPkg.getItemAt(cbPkg.getSelectedIndex());
		if(pl == null) {
			//Reset the box and return.
			setPackageCmbx(now_asset.getPackageIndex());
			return;
		}
		
		MebPackage p = pl.p;
		if(p == null) return;
		
		proj.moveAssetToPackage(now_asset, p.getIndex());
		if(browse_mode == BVIEW_TAB_PKG) updateBrowsePane();
	}
	
	protected void onAssetPane_toggleExport() {
		//Match txtbox enabling with checkbox selection
		//Populate textbox with auto generated export name, if enabled
		
		if(!cbExport.isSelected()) {
			//Disable textbox, don't clear textbox, but set asset export name to null.
			txtExport.setEnabled(false);
			if(now_asset == null) return;
			now_asset.setExportName(null);
			txtExport.repaint();
		}
		else {
			txtExport.setEnabled(true);
			if(now_asset == null) return;
			
			//Check the string in the textbox and make sure it is compliant.
			//Then set.
			//If textbox is empty, generate.
			
			if(txtExport.getText().isEmpty()) {
				String name = now_asset.getLocalName();
				StringBuilder sb = new StringBuilder(name.length()+1);
				for(int i = 0; i < name.length(); i++) {
					char c = name.charAt(i);
					if(c < 0x20 || c > 0x7e) c = '?';
					if(c == 0x20) c = '_';
					sb.append(c);
				}
				txtExport.setText(sb.toString());
				now_asset.setExportName(txtExport.getText());
			}
			else {
				String name = txtExport.getText();
				StringBuilder sb = new StringBuilder(name.length()+1);
				for(int i = 0; i < name.length(); i++) {
					char c = name.charAt(i);
					if(c < 0x20 || c > 0x7e) c = '?';
					if(c == 0x20) c = '_';
					sb.append(c);
				}
				txtExport.setText(sb.toString());
				now_asset.setExportName(txtExport.getText());
			}
			txtExport.repaint();
		}
		
	}
	
	protected void onAssetPane_browseSource() {
		if(now_asset == null) return;
		String lastpath = MuenProgramFiles.getIniValue(MuenProgramFiles.INIKEY_LAST_SRC);
		
		JFileChooser fc = new JFileChooser(lastpath);
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		
		int retVal = fc.showOpenDialog(this);
		if (retVal == JFileChooser.APPROVE_OPTION){
			File f = fc.getSelectedFile();
			String p = f.getAbsolutePath();
			
			now_asset.setSourcePath(p);
			txtSrc.setText(p);
			txtSrc.repaint();
			
			MuenProgramFiles.setIniValue(MuenProgramFiles.INIKEY_LAST_SRC, p);
		}
	}
	
	protected void onAssetPane_editAsset() {
		if(edit_callback == null) {
			showError("Edit window not defined for selected asset type.");
			return;
		}
		
		if(!edit_callback.isEditable()) {
			showError("GUI editing not enabled for selected asset type.");
			return;
		}
		
		edit_callback.editButtonCallback(this, now_asset);
	}
	
	//	-- File Menu
	
	protected void onFile_new() {

		if(proj != null) {
			int confirm = JOptionPane.showConfirmDialog(this, "Save current project before closing?", 
					"New Project", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(confirm == JOptionPane.YES_OPTION) onFile_save();
		}
		
		NewProjDialog dia = new NewProjDialog(this);
		dia.setVisible(true);
		
		int sel = dia.getCloseSelection();
		if(sel == 0) return;
		
		waitDialog = new IndefProgressDialog(this, "New Project");
		waitDialog.setPrimaryString("Please Wait");
		waitDialog.setSecondaryString("Creating new project");
		
		SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){

			protected Void doInBackground() throws Exception {
				try{
					String gc = dia.getGamecode();
					String path = dia.getPath();
					
					MuenProject p = MuenProject.newProject(gc, path);
					proj = p;
					now_asset = null;
					
					updateToProject();
					updateToAsset();
				}
				catch(Exception x){
					x.printStackTrace();
					showError("Unknown Error: Refresh Failed! See stderr for details.");
				}
				
				return null;
			}
			
			public void done()	{
				waitDialog.closeMe();
				waitDialog = null;
			}
		};
		
		task.execute();
		waitDialog.render();
		
	}
	
	protected void onFile_open() {
		if(proj != null) {
			int confirm = JOptionPane.showConfirmDialog(this, "Save current project before closing?", 
					"Open Project", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(confirm == JOptionPane.YES_OPTION) onFile_save();
		}
		
		String lastpath = MuenProgramFiles.getIniValue(MuenProgramFiles.INIKEY_LAST_PROJ);
		JFileChooser fc = new JFileChooser(lastpath);
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fc.addChoosableFileFilter(new FileFilter() {

			public boolean accept(File f) {
				if(f.isDirectory()) return true;
				String n = f.getName().toLowerCase();
				return n.endsWith("." + MuenProject.FE_PROJ);
			}

			public String getDescription() {
				return "Mu Engine Builder Project (." + MuenProject.FE_PROJ + ")";
			}
			
		});
		
		int retVal = fc.showOpenDialog(this);
		String p = null;
		if (retVal == JFileChooser.APPROVE_OPTION){
			File f = fc.getSelectedFile();
			p = f.getAbsolutePath();
			MuenProgramFiles.setIniValue(MuenProgramFiles.INIKEY_LAST_PROJ, p);
		}
		else return;
		
		waitDialog = new IndefProgressDialog(this, "Open Project");
		waitDialog.setPrimaryString("Please Wait");
		waitDialog.setSecondaryString("Opening \"" + p + "\"");
		
		String ppath = p;
		SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){

			protected Void doInBackground() throws Exception {
				try{
					
					MuenProject mp = MuenProject.readProject(ppath);
					
					proj = mp;
					now_asset = null;
					proj_path = ppath;
					
					updateToProject();
					updateToAsset();
				}
				catch(IOException x){
					x.printStackTrace();
					showError("I/O Error: Project could not be opened! See stderr for details.");
				}
				catch(UnsupportedFileTypeException x){
					x.printStackTrace();
					showError("Parser Error: Project could not be read! See stderr for details.");
				}
				catch(Exception x){
					x.printStackTrace();
					showError("Unknown Error: Open Failed! See stderr for details.");
				}
				
				return null;
			}
			
			public void done()	{
				waitDialog.closeMe();
				waitDialog = null;
			}
		};
		
		task.execute();
		waitDialog.render();
		
	}
	
	protected void onFile_save() {
		if(proj == null) return;
		//Saves to currently set path
		if(proj_path == null || proj_path.isEmpty()) {
			onFile_saveas(); return;
		}
		
		waitDialog = new IndefProgressDialog(this, "Save Project");
		waitDialog.setPrimaryString("Please Wait");
		waitDialog.setSecondaryString("Saving project to \"" + proj_path + "\"");
		
		SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){

			protected Void doInBackground() throws Exception {
				try{
					proj.writeProjFile(proj_path);
					proj.writeInitBin();
				}
				catch(IOException x){
					x.printStackTrace();
					showError("I/O Error: Save file(s) could not be written to! See stderr for details.");
				}
				catch(Exception x){
					x.printStackTrace();
					showError("Unknown Error: Save Failed! See stderr for details.");
				}
				
				return null;
			}
			
			public void done()	{
				waitDialog.closeMe();
				waitDialog = null;
			}
		};
		
		task.execute();
		waitDialog.render();
		
	}
	
	protected void onFile_saveas() {
		String lastpath = MuenProgramFiles.getIniValue(MuenProgramFiles.INIKEY_LAST_PROJ);
		JFileChooser fc = new JFileChooser(lastpath);
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fc.addChoosableFileFilter(new FileFilter() {

			public boolean accept(File f) {
				if(f.isDirectory()) return true;
				String n = f.getName().toLowerCase();
				return n.endsWith("." + MuenProject.FE_PROJ);
			}

			public String getDescription() {
				return "Mu Engine Builder Project (." + MuenProject.FE_PROJ + ")";
			}
			
		});
		
		int retVal = fc.showSaveDialog(this);
		String p = null;
		if (retVal == JFileChooser.APPROVE_OPTION){
			File f = fc.getSelectedFile();
			p = f.getAbsolutePath();
			if(p == null || p.isEmpty()) return;
			MuenProgramFiles.setIniValue(MuenProgramFiles.INIKEY_LAST_PROJ, p);
			proj_path = p;
			onFile_save();
		}
		
	}
	
	//	-- Project Menu
	
	protected void onProject_info() {
		//TODO
	}
	
	protected void onProject_packages() {
		//TODO
	}
	
	protected void onProject_groups() {
		//TODO
	}
	
	protected void onProject_import() {
		//TODO
	}
	
	protected void onProject_build() {
		//TODO
	}
	
	//	-- Editor Menu
	
	protected void onEditor_LYO() {
		//TODO
	}
	
	protected void onEditor_TXB() {
		//TODO
	}
	
	protected void onEditor_TLM() {
		//TODO
	}
	
	protected void onEditor_ANM() {
		//TODO
	}
	
	protected void onEditor_SCE() {
		//TODO
	}

	/*----- Error Messages -----*/
	
	public void showWarning(String text){
		JOptionPane.showMessageDialog(this, text, "Warning", JOptionPane.WARNING_MESSAGE);
	}
	
	public void showError(String text){
		JOptionPane.showMessageDialog(this, text, "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	public void showInfo(String text){
		JOptionPane.showMessageDialog(this, text, "Notice", JOptionPane.INFORMATION_MESSAGE);
	}

}
