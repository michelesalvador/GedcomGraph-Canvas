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
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;
import graph.gedcom.AncestryNode;
import graph.gedcom.MiniCard;
import graph.gedcom.UnitNode;
import graph.gedcom.Graph;
import graph.gedcom.IndiCard;
import graph.gedcom.Util;
import graph.gedcom.Line;
import graph.gedcom.Node;
import graph.gedcom.ProgenyNode;
import static graph.gedcom.Util.pr;

public class Diagram {

	Graph graph;
	String fulcrumId;
	JScrollPane scrollPane;
	JPanel box;
	static int shiftX = 30;
	static int shiftY = 30;

	Diagram() throws Exception {
		
		// Redefine spacing constants
		Util.PADDING = 10;
		Util.SPACE = 70;
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
		File file = new File("src/main/resources/tree2.ged");
		Gedcom gedcom = new ModelParser().parseGedcom(file);
		gedcom.createIndexes();

		// Directly open a Json file
		//String content = FileUtils.readFileToString(new File("src/main/resources/tree2.json"), "UTF-8");
		//Gedcom gedcom = new JsonParser().fromJson(content);

		// Create the diagram model from the Gedcom object
		graph = new Graph(gedcom);
		graph.showFamily(0).maxAncestors(3).maxUncles(2).displaySiblings(true).maxDescendants(3);
		fulcrumId = "I1";

		paintDiagram();
	}

	public static void main(String[] args) throws Exception {
		 new Diagram();
	}

	private void paintDiagram() {

		if (!graph.startFrom(fulcrumId)) {
			JOptionPane.showMessageDialog(null, "Can't find a person with this id.");
			return;
		}

		// Place the nodes on the canvas in random position
		box.setLayout(new OverlayLayout(box)); // This layout let the nodes auto-size
		for (Node node : graph.getNodes()) {
			if (node instanceof UnitNode)
				box.add(new GraphicUnitNode((UnitNode) node));
			else if (node instanceof AncestryNode)
				box.add(new GraphicAncestry((AncestryNode) node));
			else if (node instanceof ProgenyNode)
				box.add(new GraphicProgeny((ProgenyNode) node));
		}
		box.validate(); // To calculate the dimensions of child componenets

		// Get the dimensions of various nodes
		for (Component compoNode : box.getComponents()) {
			if (compoNode instanceof GraphicUnitNode) {
				GraphicUnitNode graphicUnitNode = (GraphicUnitNode)compoNode;
				// Get bond width
				if(graphicUnitNode.unitNode.isCouple()) {
					Bond bond = (Bond) graphicUnitNode.getComponent(0);
					graphicUnitNode.unitNode.bondWidth = bond.getWidth();
				}
				// Get the dimensions of each card
				for (Component compoCard : graphicUnitNode.getComponents()) {
					IndiCard card = null;
					if (compoCard instanceof GraphicCard) {
						card = ((GraphicCard) compoCard).card;
					} else if (compoCard instanceof Asterisk) {
						card = ((Asterisk) compoCard).card;
					}
					if(card != null) {
						card.width = compoCard.getWidth();
						card.height = compoCard.getHeight();
					}
				}
			} // Get the dimensions of each ancestry node
			else if (compoNode instanceof GraphicAncestry) {
				GraphicAncestry ancestry = (GraphicAncestry) compoNode;
				ancestry.node.width = compoNode.getWidth();
				ancestry.node.height = compoNode.getHeight();
				// Additionally set the relative X center
				if (ancestry.node.isCouple())
					ancestry.node.horizontalCenter = ancestry.getComponent(0).getWidth() + ancestry.getComponent(1).getWidth() / 2;
				else
					ancestry.node.horizontalCenter = compoNode.getWidth() / 2;
			} // Get the dimensions of each progeny node
			else if (compoNode instanceof GraphicProgeny) {
				GraphicProgeny graphicProgeny = (GraphicProgeny) compoNode;
				ProgenyNode progeny = graphicProgeny.progenyNode;
				progeny.width = graphicProgeny.getWidth();
				for(int p=0; p <graphicProgeny.getComponentCount(); p += 2) {
					Component component = graphicProgeny.getComponent(p);
					progeny.miniChildren.get(p/2).width = component.getWidth();
				}
			}
		}

		// Let the diagram calculate positions of Nodes and Lines
		graph.arrange();

		box.setLayout(null); // This non-layout let the nodes in absolute position
		box.setPreferredSize(new Dimension((int)graph.width + shiftX * 2, (int)graph.height + shiftY * 2));

		// Draw the lines
		box.add(new GraphicLines());

		// Place the nodes in definitve position on the canvas
		for (Component compoNode : box.getComponents()) {
			if (compoNode instanceof GraphicUnitNode) {
				UnitNode unitNode = ((GraphicUnitNode) compoNode).unitNode;
				//compoNode.setLocation(unitNode.x + shiftX, unitNode.y + shiftY);
				//compoNode.setSize(unitNode.width, compoNode.getHeight());
				compoNode.setBounds((int)unitNode.x + shiftX, (int)unitNode.y + shiftY, (int)unitNode.width, compoNode.getHeight());
				//
				GraphicUnitNode graphicUnitNode = (GraphicUnitNode) compoNode;
				if (unitNode.isCouple()) {
					graphicUnitNode.setLayout(null); // In alternativa potrei usare semplicemente un BoxLayout.X_AXIS
					Component husband = graphicUnitNode.getComponent(1);
					husband.setLocation(0, husband.getY());
					Component bond = graphicUnitNode.getComponent(0);
					bond.setLocation((int)unitNode.husband.width, (int)unitNode.height/2);
					Component wife = graphicUnitNode.getComponent(2);
					//wife.setLocation(unitNode.husband.width+bond.getWidth()- (unitNode.marriageDate!=null?Util.TIC*2:0),wife.getY()); //ok
					wife.setLocation((int)unitNode.husband.width + bond.getWidth(), wife.getY());
				}
			} else if (compoNode instanceof GraphicAncestry) {
				AncestryNode ancestry = (AncestryNode) ((GraphicAncestry) compoNode).node;
				compoNode.setLocation((int)ancestry.x + shiftX, (int)ancestry.y + shiftY);
			} else if (compoNode instanceof GraphicProgeny) {
				ProgenyNode progeny = (ProgenyNode) ((GraphicProgeny) compoNode).progenyNode;
				compoNode.setLocation((int)progeny.x + shiftX, (int)progeny.y + shiftY);
			}
		}
		scrollPane.validate(); // Update scrollbars
		// pr(graph.toString());
		box.repaint(); // Redraw all the canvas
	}
	
	// Graphical rappresentation of a unit node
	class GraphicUnitNode extends JPanel {
		UnitNode unitNode;
		GraphicUnitNode(UnitNode unitNode) {
			this.unitNode = unitNode;
			//setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setLayout( new OverlayLayout(this) ); // Admit overlapping of components
			//setBorder(BorderFactory.createLineBorder(Color.cyan, 1));
			setOpaque(false);
			// Create the cards
			if (unitNode.isCouple())
				add(new Bond(unitNode));	
			if (unitNode.husband != null)
				add(unitNode.husband.asterisk ? new Asterisk(unitNode.husband) : new GraphicCard(unitNode.husband));
			if (unitNode.wife != null)
				add(unitNode.wife.asterisk ? new Asterisk(unitNode.wife) : new GraphicCard(unitNode.wife));
		}
	}
	
	// Graphical realization of an individual card
	class GraphicCard extends JButton {
		IndiCard card;
		GraphicCard(IndiCard card) {
			super(Util.essence(card.person));
			this.card = card;
			setFont(new Font("Segoe UI", Font.PLAIN, 11));
			Color backgroundColor = Color.white;
			if (card.person.getId().equals(graph.getStartId())) {
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
					if (card.person.getId().equals(fulcrumId))
						JOptionPane.showMessageDialog(null, card.person.getId()+": "+Util.essence(card.person));
					else {
						box.removeAll();
						fulcrumId = card.person.getId();
						paintDiagram();
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
		IndiCard card;
		Asterisk(IndiCard card) {
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

	class Bond extends JPanel {
		UnitNode node;
		private int width = Util.MARGIN, height = 40;
		Bond(UnitNode unitNode) {
			node = unitNode;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(Box.createRigidArea(new Dimension(width, height)));
			//setBorder(BorderFactory.createLineBorder(Color.red, 1));
			addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                	JOptionPane.showMessageDialog(null, "Family " + node.family.getId());
                }
            });
		}
		@Override
		protected void paintComponent(Graphics g) {
			if (node.isCouple()) {
				// Draw the vertical line from marriage
				if (node.hasChildren()) {
					g.setColor(Color.lightGray);
					g.drawLine(width/2, 0, width/2, height);
				}
				// Draw the marriage
				if (node.marriageDate != null) {
					g.setColor(new Color(0xDDBBFF));
					g.fillOval(0, 0, width, 17);
					g.setColor(Color.black);
					g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
					g.drawString(node.marriageYear(), 0, 12);
				} else {
					// Draw a simple horizontal line
					g.setColor(Color.lightGray);
					g.drawLine(0, 0, width, 0);
				}
			}
		}
	}
	
	// Container for the ancestor minicards
	class GraphicAncestry extends JPanel {
		AncestryNode node;
		GraphicAncestry(AncestryNode node) {
			this.node = node;
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			//setBorder(BorderFactory.createLineBorder(Color.cyan, 1));
			if (node.miniFather != null)
				add(new GraphicMiniCard(node.miniFather, node.acquired));
			if (node.isCouple())
				add(Box.createRigidArea(new Dimension(20, 0)));
			if (node.miniMother != null)
				add(new GraphicMiniCard(node.miniMother, node.acquired));
			if (node.acquired)
				setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
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
	}

	class GraphicProgeny extends JPanel {
		ProgenyNode progenyNode;
		GraphicProgeny(ProgenyNode progenyNode) {
			this.progenyNode = progenyNode;
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setOpaque(false);
			//setBorder(BorderFactory.createLineBorder(Color.cyan, 1));
			for( MiniCard miniChild : progenyNode.miniChildren) {
				add(new GraphicMiniCard(miniChild, false));
				add(Box.createRigidArea(new Dimension(Util.PLAY, 0)));
			}
			this.remove(this.getComponentCount()-1);
		}
	}
	
	class GraphicMiniCard extends JButton {
		GraphicMiniCard(MiniCard miniCard, boolean acquired) {
			super(String.valueOf(miniCard.amount));
			setFont(new Font("Segoe UI", Font.PLAIN, 11));
			Color backgroundColor = acquired ? new Color(0xCCCCCC) : Color.white;
			setBackground(backgroundColor);
			Color borderColor = Color.gray;
			if (Util.sex(miniCard.person) == 1)
				borderColor = Color.blue;
			else
				borderColor = Color.pink;
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor, 1),
					BorderFactory.createLineBorder(backgroundColor, 4)));
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					box.removeAll();
					fulcrumId = miniCard.person.getId();
					paintDiagram();
				}
			});
		}
	}

	class GraphicLines extends JPanel {
		GraphicLines() {
			setBounds(shiftX, shiftY, (int)graph.width, (int)graph.height);
			//setBorder(BorderFactory.createLineBorder(Color.green, 1));
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