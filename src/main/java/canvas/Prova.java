package canvas;

import static graph.gedcom.Util.pr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.OverlayLayout;
import javax.swing.ScrollPaneConstants;
import javax.swing.ScrollPaneLayout;

public class Prova {

	//JFrame frame;
	//JPanel panel;

	public Prova() {
		// Create the JFrame
		/*
		 * frame = new JFrame(); frame.setSize(800, 500);
		 * frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.setLayout(null);
		 * frame.getContentPane().setBackground(Color.darkGray); frame.setVisible(true);
		 * 
		 * panel = new Panel(); frame.getContentPane().add(panel);
		 * 
		 * 
		 * panel.add(new JButton("Pinco pallo")); panel.add(new JButton("Seconda"));
		 * panel.revalidate();
		 * 
		 * //panel.setPreferredSize( panel.getPreferredSize() ); //frame.pack();
		 * 
		 * panel.setLocation(300, 200); pr(panel.getWidth(),panel.getHeight());
		 * //panel.invalidate(); //panel.repaint();
		 * 
		 * EventQueue.invokeLater(() -> { panel.updateUI(); });
		 */

		JFrame frame = new JFrame();
		frame.setSize(100, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(),BoxLayout.LINE_AXIS)); //ok
		//frame.getContentPane().setLayout(new GroupLayout(frame.getContentPane())); no
		
		JPanel box = new JPanel();
		//box.setLayout(new BoxLayout(box,BoxLayout.X_AXIS)); // ok
		box.setLayout( new OverlayLayout(box));
		//box.setLayout(null); // richiede di settare posizione e DIMENSIONI dei componenti figli
		box.setBackground(Color.gray);
		
		JScrollPane scrollPane = new JScrollPane(box);
		//scrollPane.setLayout(new ScrollPaneLayout()); è di default
		frame.getContentPane().add(scrollPane);
		//frame.pack();
		frame.setVisible(true);
		
		JPanel panel = new JPanel();
		panel.setBackground(Color.red);
		panel.setLayout( new BoxLayout(panel,BoxLayout.X_AXIS) );
		final JButton button1 = new JButton("Primo!!!!..........");
		panel.add(button1);
		panel.add(Box.createRigidArea(new Dimension(10,0)));
		final JButton button2 = new JButton("Sgondo");
		panel.add(button2);
		//panel.setSize(200, 40);
		box.add(panel);
		
		//panel.revalidate();
		//box.validate();
		panel.doLayout();
		pr(panel.getWidth(), panel.getHeight());
		for (Component component : panel.getComponents()) {
			pr(">>",component.getWidth(), component.getHeight());
		}
		
		button1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//box.setLayout(null);
				//box.setSize(200,700);
				box.setPreferredSize(new Dimension(200,700));
				pr(panel.getWidth(), panel.getHeight());
				panel.setLocation(20, 100);
			}
		});
		button2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				box.setLayout(new BoxLayout(box,BoxLayout.LINE_AXIS));
				panel.setLocation(10, 120);
			}
		});

	}

	class Panel extends JPanel {
		Panel() {
			setBounds(10, 10, 350, 100);
			setBackground(Color.red);
			// setLayout( new BoxLayout(this, BoxLayout.LINE_AXIS) );
			setLayout(new GridLayout(0, 3));
		}
	}
}
