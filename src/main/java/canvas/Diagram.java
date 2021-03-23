package canvas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.CubicCurve2D;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.OverlayLayout;
import javax.swing.Timer;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;
import graph.gedcom.PersonNode;
import graph.gedcom.CurveLine;
import graph.gedcom.FamilyNode;
import graph.gedcom.Graph;
import graph.gedcom.Util;
import graph.gedcom.Line;
import graph.gedcom.Node;
import static graph.gedcom.Util.*;

public class Diagram {

	Graph graph;
	Person fulcrum;
	Person firstFulcrum;
	JScrollPane scrollPane;
	JPanel box;
	Component lines;
	static int shiftX = 60;
	static int shiftY = 40;
	private Timer timer;

	Diagram() throws Exception {
		
		// Redefine spacing constants
		VERTICAL_SPACE = 70;
		FAMILY_WIDTH = 30;
		MARRIAGE_HEIGHT = 20;
		
		// Swing stuff
		JFrame frame = new JFrame();
		frame.setSize(Toolkit.getDefaultToolkit().getScreenSize());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// Header
		JPanel header = new JPanel();
		frame.getContentPane().add(header, BorderLayout.PAGE_START);
		
		// A button to play the timer
		JButton buttonPlay = new JButton("Play");
		header.add(buttonPlay);
		buttonPlay.setBounds(300, 10, 100, 30);
		buttonPlay.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (timer.isRunning()) {
					timer.stop();
				} else {
					timer.start();
				}
			}
		});
		
		// A button to reset the diagram
		JButton buttonReset = new JButton("Reset");
		header.add(buttonReset);
		buttonReset.setBounds(150, 10, 100, 30);
		buttonReset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( timer.isRunning() ) {
					timer.stop();
				}
				box.removeAll();
				fulcrum = firstFulcrum;
				startDiagram();
			}
		});

		box = new JPanel();
		box.setBackground(Color.darkGray);
		scrollPane = new JScrollPane(box);
		frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
		frame.setVisible(true);
		
		// Parse a Gedcom file
	/*	File file = new File("src/main/resources/tree2.ged");
		Gedcom gedcom = new ModelParser().parseGedcom(file);
		gedcom.createIndexes();*/

		// Directly open a Json file
		String content = FileUtils.readFileToString(new File("src/main/resources/overlap.json"), "UTF-8");
		Gedcom gedcom = new JsonParser().fromJson(content);

		// Create the diagram model from the Gedcom object
		graph = new Graph(gedcom);
		graph.showFamily(0).maxAncestors(5).maxGreatUncles(2).displaySpouses(true).maxDescendants(3).maxSiblingsNephews(0).maxUnclesCousins(0);
		fulcrum = gedcom.getPerson("I1");
		firstFulcrum = fulcrum;

		timer = new Timer(40, e -> {  // 40 = 25 fps
			if( graph.playNodes() ) // Calculate next position
				displaceDiagram();
			else
				timer.stop();
		});
		
		startDiagram();
	}

	public static void main(String[] args) throws Exception {
		new Diagram();
	}

	// Initialize the diagram the first time and clicking on a card
	private void startDiagram() {

		// Let the diagram find the persons to display
		graph.startFrom(fulcrum);

		// Place the nodes on the canvas without position
		box.setLayout(new OverlayLayout(box)); // This layout let the nodes auto-size
		//box.setLayout(null);
		for( Node node : graph.getNodes() ) {
			if( node instanceof PersonNode ) {
				PersonNode personNode = (PersonNode)node;
				if( personNode.person.equals(fulcrum) && !personNode.isFulcrumNode() )
					box.add(new Asterisk(personNode));
				else if( personNode.mini )
					box.add(new GraphicMiniCard(personNode));
				else
					box.add(new GraphicPerson(personNode));
			}
		}
		box.validate(); // To calculate the dimensions of child componenets

		// Get the dimensions of each node
		for( Component compoNode : box.getComponents() ) {
			Node node = ((GraphicNode)compoNode).node;
			node.width = compoNode.getWidth();
			node.height = compoNode.getHeight();
		}

		// Let the diagram initialize nodes and lines
		graph.initNodes();

		box.setLayout(null); // This non-layout let the nodes in absolute position
		
		// Add marriages nodes
		graph.getNodes().forEach(node -> {
			if( node instanceof FamilyNode ) {
				box.add(new GraphicFamily((FamilyNode)node), 0);
			}
		});
		
		// First raw calculation of nodes position
		graph.placeNodes();
		
		// Add the lines
		lines = box.add(new GraphicLines());

		// First visible displacement of the diagram
		displaceDiagram();
		box.repaint(); // Clears dirty

		timer.start();
	}

	// Visible position of nodes and lines
	void displaceDiagram() {
		for( Component compoNode : box.getComponents() ) {
			if( compoNode instanceof GraphicNode ) {
				Node node = ((GraphicNode)compoNode).node;
				compoNode.setLocation((int)node.x + shiftX, (int)node.y + shiftY);
				//compoNode.setSize(unitNode.width, compoNode.getHeight());
			}
		}
		lines.setBounds(shiftX, shiftY, (int)graph.getWidth(), (int)graph.getHeight());
		lines.repaint();
		box.setPreferredSize(new Dimension((int)graph.getWidth() + shiftX * 2, (int)graph.getHeight() + shiftY * 2));
		scrollPane.validate(); // Update scrollbars
	}

	// Graphical rappresentation of a single node
	abstract class GraphicNode extends JLabel {
		Node node;
		GraphicNode(Node node) {
			this.node = node;
			//setBorder(BorderFactory.createLineBorder(Color.cyan, 1));
			setLayout(new OverlayLayout(this)); // Admit overlapping of components
			setFont(new Font("Segoe UI", Font.PLAIN, 11));
			setOpaque(true);
			addMouseListener(new MouseAdapter() { // Mouse down / mouse up
				@Override
				public void mousePressed(MouseEvent e) {
					node.drag = true;
				}
				@Override
				public void mouseReleased(MouseEvent e) {
					node.drag = false;
				}
			});
			addMouseMotionListener(new MouseAdapter() { // Mouse drag
				@Override
				public void mouseDragged(MouseEvent e) {
					float x = getX() + e.getX() - shiftX;
					float y = getY() + e.getY() - shiftY;
					node.x = x;
					node.y = y;
				}
			});
		}
	}

	// Graphical realization of an individual card
	class GraphicPerson extends GraphicNode {
		GraphicPerson(PersonNode node) {
			super(node);
			setText(/*graph.getNodes().indexOf(node) +" "+*/ node.toString());
			Color backgroundColor = Color.white;
			if (node.person.equals(fulcrum)) {
				backgroundColor = Color.orange;
			} else if (node.acquired) {
				backgroundColor = new Color(0xCCCCCC);
			}
			setBackground(backgroundColor);
			Color borderColor = Color.gray;
			if (Util.sex(node.person) == 1) {
				borderColor = Color.blue;
			} else if (Util.sex(node.person) == 2) {
				borderColor = Color.pink;
			}
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor, 2),
					BorderFactory.createLineBorder(backgroundColor, 10)));
			addMouseListener( new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (node.person.equals(fulcrum))
						JOptionPane.showMessageDialog(null, node.person.getId()+": "+Util.essence(node.person));
					else {
						box.removeAll();
						fulcrum = node.person;
						startDiagram();
					}
				}
			});
		}
		@Override
		protected void paintBorder(Graphics g) {
			super.paintBorder(g);
			// Death ribbon
			if( ((PersonNode)node).dead ) {
				int[] pX = { (int)node.width - 12, (int)node.width - 7, (int)node.width, (int)node.width };
				int[] pY = { 0, 0, 7, 12 };
				g.fillPolygon(pX, pY, 4);
			}
		}
	}

	// Replacement for any person who is actually fulcrum
	class Asterisk extends GraphicNode {
		Asterisk(PersonNode node) {
			super(node);
			setFont(new Font("Segoe UI", Font.BOLD, 50));
			setForeground(Color.orange);
			setText("*");
			setOpaque(false);
			//setBorder(BorderFactory.createLineBorder(Color.red, 1));
			addMouseListener( new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					JOptionPane.showMessageDialog(null, node.person.getId() + ": " + node);
				}
			});
		}
	}

	class GraphicFamily extends GraphicNode {
		GraphicFamily(FamilyNode familyNode) {
			super(familyNode);
			//setBorder(BorderFactory.createLineBorder(Color.yellow, 1));
			setSize((int)node.width, (int)node.height);
			addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                	JOptionPane.showMessageDialog(null, "Family " + node.spouseFamily.getId());
                }
            });
		}
		@Override
		protected void paintComponent(Graphics g) {
			FamilyNode familyNode = (FamilyNode) node;
			// Draw the vertical line from marriage
			if (familyNode.hasChildren()) {
				g.setColor(Color.lightGray);
				g.drawLine((int)node.centerRelX(), (int)node.centerRelY(), (int)node.centerRelX(), (int)node.height);
			}
			// Draw the marriage
			if (familyNode.marriageDate != null) {
				g.setColor(new Color(0xFFFFFF));
				g.fillOval(0, 0, (int)node.width, MARRIAGE_HEIGHT);
				g.setColor(new Color(0xAAAAAA));
				g.drawOval(0, 0, (int)node.width, MARRIAGE_HEIGHT);
				g.setColor(Color.black);
				g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
				g.drawString(familyNode.marriageYear(), 0, 13);
			} else {
				g.setColor(Color.lightGray);
				int diameter = HEARTH_DIAMETER;
				if (node.mini)
					diameter = MINI_HEARTH_DIAMETER;
				g.fillOval((int)node.centerRelX()-diameter/2, (int)node.centerRelY()-diameter/2, diameter, diameter);
			}
		}
	}
	
	class GraphicMiniCard extends GraphicNode {
		GraphicMiniCard(PersonNode node) {
			super(node);
			setText(String.valueOf(node.amount));
			Color backgroundColor = node.acquired ? new Color(0xCCCCCC) : Color.white;
			setBackground(backgroundColor);
			Color borderColor = Color.gray;
			if (Util.sex(node.person) == 1)
				borderColor = Color.blue;
			else
				borderColor = Color.pink;
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor, 1),
					BorderFactory.createLineBorder(backgroundColor, 4)));
			addMouseListener( new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					box.removeAll();
					fulcrum = node.person;
					startDiagram();
				}
			});
		}
	}

	class GraphicLines extends JPanel {
		GraphicLines() {
			setBounds(shiftX, shiftY, (int)graph.getWidth(), (int)graph.getHeight());
			setBorder(BorderFactory.createLineBorder(Color.orange, 1));
			setOpaque(false);
		}
		@Override
		protected void paintComponent(Graphics g) {
			for( Line line : graph.getLines() ) {
				int x1 = (int)line.x1;
				int y1 = (int)line.y1;
				int x2 = (int)line.x2;
				int y2 = (int)line.y2;
				g.setColor(Color.lightGray);
				if( line instanceof CurveLine ) {
					Graphics2D g2 = (Graphics2D) g;
					CubicCurve2D c = new CubicCurve2D.Double();
					c.setCurve(x1, y1, x1, y2 , x2, y1, x2, y2);
					g2.draw(c);
				} else {
					g.drawLine(x1, y1, x2, y2);
				}
			}
		}
	}
}