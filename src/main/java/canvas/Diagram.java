package canvas;

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
import java.awt.event.MouseListener;
import java.awt.geom.CubicCurve2D;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
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
import graph.gedcom.FamilyNode;
import graph.gedcom.Graph;
import graph.gedcom.Util;
import graph.gedcom.Line;
import graph.gedcom.Node;
import static graph.gedcom.Util.*;

public class Diagram {

	Graph graph;
	Person fulcrum;
	JScrollPane scrollPane;
	JPanel box;
	Component lines;
	static int shiftX = 50;
	static int shiftY = 50;
	
	private Timer timer;

	Diagram() throws Exception {
		
		// Redefine spacing constants
		Util.PADDING = 10;
		Util.SPACE = 100;
		Util.TIC = 0;
		Util.GAP = 20;
		
		// Swing stuff
		JFrame frame = new JFrame();
		frame.setSize(Toolkit.getDefaultToolkit().getScreenSize());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		box = new JPanel();
		//box.setLayout(new BoxLayout(box, BoxLayout.X_AXIS));
		//box.setLayout( new OverlayLayout(box));	// Allow absolute positioning of nodes
		box.setBackground(Color.darkGray);
		scrollPane = new JScrollPane(box);
		frame.getContentPane().add(scrollPane);
		frame.setVisible(true);
		
		// Parse a Gedcom file
	/*	File file = new File("src/main/resources/tree2.ged");
		Gedcom gedcom = new ModelParser().parseGedcom(file);
		gedcom.createIndexes();*/

		// Directly open a Json file
		String content = FileUtils.readFileToString(new File("src/main/resources/tree2.json"), "UTF-8");
		Gedcom gedcom = new JsonParser().fromJson(content);

		// Create the diagram model from the Gedcom object
		graph = new Graph(gedcom);
		graph.showFamily(0).maxAncestors(0).maxGreatUncles(0).displaySpouses(true).maxDescendants(0).maxSiblingsNephews(0).maxUnclesCousins(0);
		fulcrum = gedcom.getPerson("I1");

		startDiagram();
		
		timer = new Timer(40, e -> playDiagram());
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
		for (Node node : graph.getNodes()) {
			box.add(new GraphicNode(node));
		}
		box.validate(); // To calculate the dimensions of child componenets

		// Get the dimensions of each node
		for (Component compoNode : box.getComponents()) {
			Node node = ((GraphicNode)compoNode).node;
			node.width = compoNode.getWidth();
			node.height = compoNode.getHeight();
		}

		// Let the diagram initialize positions of nodes and lines
		graph.arrangeNodes();

		box.setLayout(null); // This non-layout let the nodes in absolute position
		
		// Add the lines
		lines = box.add(new GraphicLines());

		// A button to play the timer
		JButton button = new JButton("Play");
		box.add(button);
		button.setBounds(300, 10, 100, 30);
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (timer.isRunning())
					timer.stop();
				else
					timer.start();
			}
		});
		
		// Just one first movement of the diagram
		playDiagram();
	}
	
	void playDiagram() {
		graph.playNodes();
		for (Component compoNode : box.getComponents()) {
			if (compoNode instanceof GraphicNode) {
				Node node = ((GraphicNode) compoNode).node;
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
	class GraphicNode extends JPanel {
		Node node;
		GraphicNode(Node node) {
			this.node = node;
			setLayout(new OverlayLayout(this)); // Admit overlapping of components
			//setBorder(BorderFactory.createLineBorder(Color.red, 1));
			setOpaque(false);
			// Create various type of the nodes
			if (node instanceof FamilyNode)
				add(new GraphicFamily((FamilyNode)node));
			else if (((PersonNode)node).person.equals(fulcrum) && !((PersonNode)node).isFulcrumNode())
				add(new Asterisk((PersonNode)node));
			else if (((PersonNode)node).mini)
				add(new GraphicMiniCard((PersonNode)node));
			else
				add(new GraphicPerson((PersonNode) node));
		}
	}

	// Graphical realization of an individual card
	class GraphicPerson extends JButton {
		PersonNode card;
		GraphicPerson(PersonNode card) {
			super(Util.essence(card.person));
			this.card = card;
			setFont(new Font("Segoe UI", Font.PLAIN, 11));
			Color backgroundColor = Color.white;
			if (card.person.equals(fulcrum)) {
				backgroundColor = Color.orange;
			} else if (card.acquired) {
				backgroundColor = new Color(0xCCCCCC);
			}
			setBackground(backgroundColor);
			Color borderColor = Color.gray;
			if (Util.sex(card.person) == 1) {
				borderColor = Color.blue;
			} else if (Util.sex(card.person) == 2) {
				borderColor = Color.pink;
			}
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor, 2),
					BorderFactory.createLineBorder(backgroundColor, 10)));
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (card.person.equals(fulcrum))
						JOptionPane.showMessageDialog(null, card.person.getId()+": "+Util.essence(card.person));
					else {
						box.removeAll();
						fulcrum = card.person;
						startDiagram();
					}
				}
			});
		}
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			// Death ribbon
			g.setColor(Color.black);
			if (card.dead) {
				int[] pX = { (int)card.width - 12, (int)card.width - 7, (int)card.width, (int)card.width };
				int[] pY = { 0, 0, 7, 12 };
				g.fillPolygon(pX, pY, 4);
			}
		}
	}

	// Replacement for person with multiple marriages
	class Asterisk extends JPanel {
		PersonNode card;
		Asterisk(PersonNode card) {
			this.card = card;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(Box.createRigidArea(new Dimension(20, 25)));
			//setBorder(BorderFactory.createLineBorder(Color.cyan, 1));
			addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					JOptionPane.showMessageDialog(null, card.person.getId()+": "+Util.essence(card.person));
				}
				@Override
				public void mouseEntered(MouseEvent arg0) {}
				@Override
				public void mouseExited(MouseEvent arg0) {}
				@Override
				public void mousePressed(MouseEvent arg0) {}
				@Override
				public void mouseReleased(MouseEvent arg0) {}
			});
		}
		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(Color.orange);
			g.setFont(new Font("Segoe UI", Font.BOLD, 40));
			g.drawString("*", 0, 35);
		}
	}

	class GraphicFamily extends JPanel {
		FamilyNode node;
		private int width, height;
		GraphicFamily(FamilyNode node) {
			//setBorder(BorderFactory.createLineBorder(Color.yellow, 1));
			setBackground(Color.white);
			this.node = node;
			if(node.mini) {
				width = Util.MINI_MARGIN;
				height = 20;
			} else {
				width = Util.MARGIN;
				height = 40;
			}
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(Box.createRigidArea(new Dimension(width, height)));
			//setBorder(BorderFactory.createLineBorder(Color.yellow, 1));
			if (!node.mini)
				addMouseListener(new MouseAdapter() {
	                @Override
	                public void mouseReleased(MouseEvent e) {
	                	JOptionPane.showMessageDialog(null, "Family " + node.spouseFamily.getId());
	                }
	            });
		}
		@Override
		protected void paintComponent(Graphics g) {
			// Draw the vertical line from marriage
			if (node.hasChildren()) {
				g.setColor(Color.lightGray);
				g.drawLine(width/2, height/2, width/2, height);
			}
			// Draw the marriage
			if (node.marriageDate != null) {
				g.setColor(new Color(0xDDBBFF));
				g.fillOval(0, 0, width, 17);
				g.setColor(Color.black);
				g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
				g.drawString(node.marriageYear(), 0, 12);
			} else {
				g.setColor(Color.lightGray);
				int radius = 10;
				if (node.mini)
					radius = 6;
				g.fillOval(width/2-radius/2, height/2-radius/2, radius, radius);
			}
			/*} else {
				// Draw a simple horizontal line
				g.setColor(Color.lightGray);
				g.drawLine(0, 0, width, 0);
			}*/
		}
	}
	
	/* Container for the ancestor minicards
	class GraphicAncestry extends JPanel {
		AncestryNode node;
		GraphicAncestry(AncestryNode node) {
			this.node = node;
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			//setBorder(BorderFactory.createLineBorder(Color.cyan, 1));
			if (node.husband != null)
				add(new GraphicMiniCard(node.husband, node.husband.acquired));
			if (node.isCouple())
				add(Box.createRigidArea(new Dimension(20, 0)));
			if (node.wife != null)
				add(new GraphicMiniCard(node.wife, node.wife.acquired));
		//	if (node.acquired)
		//		setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		}
		@Override
		protected void paintComponent(Graphics g) {
			AncestryNode node = (AncestryNode) this.node;
			// Draw the T lines
			if (node.isCouple()) {
				g.setColor(Color.lightGray);
				g.drawLine(0, (int)node.centerRelY(), (int)node.width, (int)node.centerRelY()); // Horizontal
				g.drawLine((int)node.centerRelX(), (int)node.centerRelY(), (int)node.centerRelX(), (int)node.height); // Vertical
			}
		}
	}*/

	class GraphicMiniCard extends JButton {
		PersonNode card;
		GraphicMiniCard(PersonNode card) {
			super(String.valueOf(card.amount));
			this.card = card;
			setFont(new Font("Segoe UI", Font.PLAIN, 11));
			Color backgroundColor = card.acquired ? new Color(0xCCCCCC) : Color.white;
			setBackground(backgroundColor);
			Color borderColor = Color.gray;
			if (Util.sex(card.person) == 1)
				borderColor = Color.blue;
			else
				borderColor = Color.pink;
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor, 1),
					BorderFactory.createLineBorder(backgroundColor, 4)));
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					box.removeAll();
					fulcrum = card.person;
					startDiagram();
				}
			});
		}
	}

	class GraphicLines extends JPanel {
		GraphicLines() {
		//	pr(shiftX, shiftY, (int)graph.getWidth(), (int)graph.getHeight());
			setBounds(shiftX, shiftY, (int)graph.getWidth(), (int)graph.getHeight());
			setBorder(BorderFactory.createLineBorder(Color.orange, 1));
			setOpaque(false);
		}
		@Override
		protected void paintComponent(Graphics g) {
			for (Line line : graph.getLines()) {
				int x1 = (int)line.x1;
				int y1 = (int)line.y1;
				int x2 = (int)line.x2;
				int y2 = (int)line.y2;
				Graphics2D g2 = (Graphics2D) g;
				CubicCurve2D c = new CubicCurve2D.Double();
				g.setColor(Color.lightGray);
				c.setCurve(x1, y1, x1, y2 , x2, y1, x2, y2);
				g2.draw(c);
			}
		}
	}
}