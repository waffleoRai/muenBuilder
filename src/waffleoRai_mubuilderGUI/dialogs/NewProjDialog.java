package waffleoRai_mubuilderGUI.dialogs;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Random;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JSeparator;

import waffleoRai_mubuilder.MuenProgramFiles;

public class NewProjDialog extends JDialog {

	private static final long serialVersionUID = 4816896870148851616L;
	
	public static final int WIDTH = 410;
	public static final int HEIGHT = 195;
	
	private JTextField txtGamecode;
	private JTextField txtPath;
	
	private String gc;
	private String path;
	private int close_sel;
	
	public NewProjDialog(Frame parent) {
		super(parent, true);
		super.setMinimumSize(new Dimension(WIDTH, HEIGHT));
		super.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		super.setLocationRelativeTo(parent);
		initGUI();
	}
	
	private void initGUI() {
		setResizable(false);
		setTitle("New Project");
		getContentPane().setLayout(null);
		
		JLabel lblNewLabel = new JLabel("Gamecode:");
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 12));
		lblNewLabel.setBounds(10, 22, 66, 14);
		getContentPane().add(lblNewLabel);
		
		txtGamecode = new JTextField();
		txtGamecode.setBounds(86, 20, 91, 20);
		getContentPane().add(txtGamecode);
		txtGamecode.setColumns(10);
		
		JButton btnRandom = new JButton("Random");
		btnRandom.setBounds(187, 19, 89, 23);
		getContentPane().add(btnRandom);
		btnRandom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				randomizeGamecode();
			}
		});
		
		JLabel lblNewLabel_1 = new JLabel("Build Root:");
		lblNewLabel_1.setFont(new Font("Tahoma", Font.BOLD, 12));
		lblNewLabel_1.setBounds(10, 60, 76, 14);
		getContentPane().add(lblNewLabel_1);
		
		txtPath = new JTextField();
		txtPath.setBounds(86, 58, 303, 20);
		getContentPane().add(txtPath);
		txtPath.setColumns(10);
		
		JButton btnBrowse = new JButton("Browse...");
		btnBrowse.setBounds(300, 89, 89, 23);
		getContentPane().add(btnBrowse);
		btnBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onBrowse();
			}
		});
		
		JButton btnOK = new JButton("OK");
		btnOK.setBounds(300, 132, 89, 23);
		getContentPane().add(btnOK);
		btnOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!checkGamecode()) {
					showError("Gamecode must consist of exactly 8 non-control, non-whitespace ASCII characters.");
					return;
				}
				
				close_sel = 1;
				closeMe();
			}
		});
		
		JButton btnCancel = new JButton("Cancel");
		btnCancel.setBounds(201, 132, 89, 23);
		getContentPane().add(btnCancel);
		btnOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				close_sel = 0;
				closeMe();
			}
		});
		
		JSeparator separator = new JSeparator();
		separator.setBounds(47, 123, 342, 5);
		getContentPane().add(separator);
	}
	
	public void onBrowse() {
		String lastpath = MuenProgramFiles.getIniValue(MuenProgramFiles.INIKEY_LAST_BUILDDIR);
		JFileChooser fc = new JFileChooser(lastpath);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		int retVal = fc.showSaveDialog(this);
		if (retVal == JFileChooser.APPROVE_OPTION){
			File f = fc.getSelectedFile();
			String p = f.getAbsolutePath();
			
			txtPath.setText(p);
			txtPath.repaint();
			
			MuenProgramFiles.setIniValue(MuenProgramFiles.INIKEY_LAST_BUILDDIR, p);
		}
	}
	
	public void closeMe() {
		gc = txtGamecode.getText();
		path = txtPath.getText();
		this.setVisible(false);
		this.dispose();
	}
	
	public int getCloseSelection() {return close_sel;}
	
	public boolean checkGamecode() {
		String code = txtGamecode.getText();
		if(code.length() != 8) return false;
		
		for(int i = 0; i < 8; i++) {
			char c = code.charAt(i);
			if(c < 0x21) return false;
			if(c > 0x7e) return false;
		}
		
		return true;
	}
	
	public void randomizeGamecode() {
		StringBuilder sb = new StringBuilder(9);
		Random r = new Random();
		for(int i = 0; i < 8; i++) {
			char c = (char)(r.nextInt(0x5e) + 0x21);
			sb.append(c);
		}
		txtGamecode.setText(sb.toString());
		txtGamecode.repaint();
	}
	
	public String getGamecode() {return gc;}
	public String getPath() {return path;}
	
	public void showError(String text){
		JOptionPane.showMessageDialog(this, text, "Error", JOptionPane.ERROR_MESSAGE);
	}
	
}
