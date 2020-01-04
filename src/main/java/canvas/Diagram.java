package canvas;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.border.Border;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
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
	JPanel box;

	static int sizeHoriz = 1600;
	static int sizeVert = 900;
	static int shiftX = 50;
	static int shiftY = 50;

	Diagram() throws Exception {

		// Swing stuff
		JFrame frame = new JFrame();
		frame.setSize(sizeHoriz, sizeVert);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		box = new JPanel();
		//box.setLayout(new BoxLayout(box, BoxLayout.X_AXIS));
		box.setLayout( new OverlayLayout(box));	// Allow absolute positioning of nodes
		box.setBackground(Color.darkGray);
		JScrollPane scrollPane = new JScrollPane(box);
		frame.getContentPane().add(scrollPane);
		frame.setVisible(true);

		// Parse a Gedcom file
		File file = new File("src/main/resources/single.ged");
		Gedcom gedcom = new ModelParser().parseGedcom(file);
		gedcom.createIndexes();

		// Directly open a Json file
		// String content = FileUtils.readFileToString(new
		// File("..\\esempi\\famiglia.ged.json"), "UTF-8");
		// Gedcom gedcom = new JsonParser().fromJson(content);

		// Create the diagram model from the Gedcom object
		graph = new Graph(gedcom);
		graph.showFamily(0).maxAncestors(2);
		fulcrumId = "I17";/**/

		paintDiagram();
	}

	public static void main(String[] args) throws Exception {
		 new Diagram();
		//new Prova();
	}

	private void paintDiagram() {

		if (!graph.startFrom(fulcrumId)) {
			JOptionPane.showMessageDialog(null, "Can't find a person with this id.");
			return;
		}

		// Place the nodes on the canvas in random position
		//box.setLayout(new BoxLayout(box, BoxLayout.X_AXIS)); // This layout let the nodes auto-size
		for (Node node : graph.getNodes()) {
			if (node instanceof UnitNode)
				box.add(new GraphicUnitNodeBox((UnitNode) node));
			else if (node instanceof AncestryNode)
				box.add(new GraphicAncestry((AncestryNode) node, false));
		}
		box.validate(); // To calculate the dimensions of child componenets

		for (Component compoNode : box.getComponents()) {
			if (compoNode instanceof GraphicUnitNodeBox) {
				GraphicUnitNode graphicUnitNode = ((GraphicUnitNodeBox)compoNode).graphicUnitNode;
				// Get bond width
				if(graphicUnitNode.node.isCouple()) {
					Bond bond = (Bond) graphicUnitNode.getComponent(0);
					graphicUnitNode.node.bondWidth = bond.getWidth();
				}
				// Get the dimensions of each card
				for (Component compoCard : graphicUnitNode.getComponents()) {
					if (compoCard instanceof GraphicCardBox) {
						GraphicCardBox graphicCard = (GraphicCardBox) compoCard;
						graphicCard.card.width = graphicCard.getWidth();
						graphicCard.card.height = graphicCard.getHeight();
					}
				}
				ProgenyNode progeny = graphicUnitNode.node.getProgeny();
				if (progeny != null) {
					GraphicProgeny graphicProgeny = ((GraphicUnitNodeBox)compoNode).graphicProgeny;
					progeny.width = graphicProgeny.getWidth();
				}
			} // Get the dimensions of each ancestry node
			else if (compoNode instanceof GraphicAncestry) {
				GraphicAncestry ancestry = (GraphicAncestry) compoNode;
				// ancestry.validate();
				ancestry.node.width = compoNode.getWidth();
				ancestry.node.height = compoNode.getHeight();
				// Additionally set the relative X center
				if (ancestry.node.isCouple())
					ancestry.node.horizontalCenter = ancestry.getComponent(0).getWidth() + ancestry.getComponent(1).getWidth() / 2;
				else
					ancestry.node.horizontalCenter = compoNode.getWidth() / 2;
			}
		}

		// Let the diagram calculate positions of Nodes and Lines
		graph.arrange();

		//box.setLayout(null); // This non-layout let the nodes in absolute position
		box.setPreferredSize(new Dimension(graph.width + shiftX * 2, graph.height + shiftY * 2));

		// Place the nodes in definitve position on the canvas
		for (Component compoNode : box.getComponents()) {
			if (compoNode instanceof GraphicUnitNodeBox) {
				UnitNode unitNode = ((GraphicUnitNodeBox) compoNode).unitNode;
				compoNode.setLocation(unitNode.x + shiftX, unitNode.y + shiftY);
				compoNode.setSize(unitNode.width, compoNode.getHeight()); 
				//compoNode.setSize(unitNode.width, compoNode.getHeight());
				//
				GraphicUnitNode graphicUnitNode = ((GraphicUnitNodeBox) compoNode).graphicUnitNode;
				if (graphicUnitNode.node.isCouple()) {
					graphicUnitNode.setSize(unitNode.width, unitNode.height); // ok ma non riimensiona il parent
					//graphicUnitNode.setPreferredSize(new Dimension(unitNode.width, unitNode.height));
					Bond bond = (Bond) graphicUnitNode.getComponent(0);
					//bond.setAlignmentX(graphicUnitNode.node.husband.width); no
					//bond.setAlignmentX(Component.RIGHT_ALIGNMENT); nno
					bond.setLocation(graphicUnitNode.node.husband.width, unitNode.height/2);
					Component wife = graphicUnitNode.getComponent(2);
					wife.setLocation(graphicUnitNode.node.husband.width+bond.getWidth(),wife.getY()); //ok					
				}
			} else if (compoNode instanceof GraphicAncestry) {
				AncestryNode ancestry = (AncestryNode) ((GraphicAncestry) compoNode).node;
				compoNode.setLocation(ancestry.x + shiftX, ancestry.y + shiftY);
			}
		}

		// Draw the lines
		box.add(new GraphicLines());

		// pr(graph.toString());
		// box.revalidate(); // Make the cards appear on the canvas
		box.repaint(); // Redraw all the canvas
	}
	
	// Container for unit node with progeny below
	class GraphicUnitNodeBox extends JPanel {
		UnitNode unitNode;
		GraphicUnitNode graphicUnitNode;
		GraphicProgeny graphicProgeny;
		GraphicUnitNodeBox(UnitNode unitNode) {
			this.unitNode = unitNode;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setOpaque(false);
			setBorder(BorderFactory.createLineBorder(Color.red, 1));
			// setBackground( Color.green );

			graphicUnitNode = new GraphicUnitNode(unitNode);
			graphicUnitNode.setAlignmentX(CENTER_ALIGNMENT);
			add(graphicUnitNode);
			ProgenyNode progeny = unitNode.getProgeny();
			if (progeny != null) {
				add(Box.createRigidArea(new Dimension(0, Util.GAP)));
				graphicProgeny = new GraphicProgeny(progeny);
				//graphicProgeny.setAlignmentX(CENTER_ALIGNMENT);
				graphicProgeny.setAlignmentX(LEFT_ALIGNMENT);
				add(graphicProgeny);
			}
		}
	}
	
	// Graphical rappresentation of a unit node
	class GraphicUnitNode extends JPanel {
		UnitNode node;
		GraphicUnitNode(UnitNode node) {
			this.node = node;
			//setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setLayout( new OverlayLayout(this) ); // Admit overlapping of components
			//this.setSize(node.width+20, node.height);
			setBorder(BorderFactory.createLineBorder(Color.cyan, 1));
			setOpaque(false);
			// Create the cards
			if (node.isCouple())
				add(new Bond(node));	
			if (node.husband != null)
				add(new GraphicCardBox(node.husband));
			if (node.wife != null) {
				GraphicCardBox wife = new GraphicCardBox(node.wife);
				//wife.setAlignmentX(-50);
				//wife.setAlignmentY(0.5f);
				add(wife);
			}
		}
	}
	
	// Container for card with ancestors above
	class GraphicCardBox extends JPanel {
		IndiCard card;
		GraphicCardBox(IndiCard card) {
			this.card = card;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setOpaque(false);
			//setBorder(BorderFactory.createLineBorder(Color.red, 1));
			// setBackground( Color.green );

			if (card.acquired && card.hasAncestry()) {
				GraphicAncestry ancestry = new GraphicAncestry((AncestryNode) card.origin, true);
				// ancestry.setAlignmentX(Component.CENTER_ALIGNMENT);
				add(ancestry);
			}
			GraphicCard graphicCard = new GraphicCard(card);
			graphicCard.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(graphicCard);
			/*if (card.acquired && card.hasAncestry())
				add(Box.createRigidArea(new Dimension(0, Util.TIC)));*/
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
			if (card.person.getId().equals(graph.getStartId()))
				backgroundColor = Color.orange;
			else if (card.acquired) {
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
					BorderFactory.createLineBorder(backgroundColor, 15)));
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					box.removeAll();
					fulcrumId = card.person.getId();
					paintDiagram();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			// Death ribbon
			g.setColor(Color.black);
			if (card.dead) {
				int[] pX = { card.width - 12, card.width - 7, card.width, card.width };
				int[] pY = { 0, 0, 7, 12 };
				g.fillPolygon(pX, pY, 4);
			}
		}
	}

	class Bond extends JPanel {
		UnitNode node;
		Bond(UnitNode unitNode) {
			node = unitNode;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			//this.setSize(20, 6); purtroppo inefficace
			add(Box.createRigidArea(new Dimension(25, 17)));
		}
		@Override
		protected void paintComponent(Graphics g) {
			if (node.isCouple()) {
				// Draw the vertical line from marriage
				if (node.guardGroup != null && !node.guardGroup.getYouths().isEmpty()) {
					g.setColor(Color.lightGray);
					g.drawLine(node.centerXrel(), node.centerYrel(), node.centerXrel(), node.height);
				}
				// Draw the marriage
				if (node.marriageDate != null) {
					int w = 25;
					int h = 17;
					int x = 0;
					int y = 0;
					g.setColor(new Color(0xDDBBFF));
					g.fillOval(x, y, w, h);
					g.setColor(Color.black);
					g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
					g.drawString(node.marriageYear(), x, y + 12);
				} else {
					// Draw a simple horizontal line
					g.setColor(Color.lightGray);
					g.drawLine(0, 0, 25, 0);
				}
			}
		}
	}
	
	// Container for the ancestor minicards
	class GraphicAncestry extends JPanel {
		AncestryNode node;
		GraphicAncestry(AncestryNode node, boolean acquired) {
			this.node = node;
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			// setBorder(BorderFactory.createLineBorder(Color.cyan, 1));
			if (node.miniFather != null)
				add(new GraphicMiniCard(node.miniFather));
			if (node.isCouple())
				add(Box.createRigidArea(new Dimension(20, 0)));
			if (node.miniMother != null)
				add(new GraphicMiniCard(node.miniMother));
			if (acquired)
				setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
		}
		@Override
		protected void paintComponent(Graphics g) {
			AncestryNode node = (AncestryNode) this.node;
			// Draw the T lines
			if (node.isCouple()) {
				g.setColor(Color.lightGray);
				g.drawLine(0, node.centerYrel(), node.width, node.centerYrel()); // Horizontal
				g.drawLine(node.centerXrel(), node.centerYrel(), node.centerXrel(), node.height); // Vertical
			}
		}
	}

	class GraphicProgeny extends JPanel {
		//ProgenyNode progenyNode;
		GraphicProgeny(ProgenyNode progenyNode) {
			//this.progenyNode = progenyNode;
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setOpaque(false);
			setBorder(BorderFactory.createLineBorder(Color.cyan, 1));
			for( MiniCard miniChild : progenyNode.miniChildren) {
				add(new GraphicMiniCard(miniChild));
				//if( progenyNode.miniChildren.get(index)  progenyNode.miniChildren.size())
				add(Box.createRigidArea(new Dimension(Util.GAP, 0)));
			}
			this.remove(this.getComponentCount()-1);
		}
	}
	
	class GraphicMiniCard extends JButton {
		GraphicMiniCard(MiniCard miniCard) {
			super(String.valueOf(miniCard.ancestry));
			setFont(new Font("Segoe UI", Font.PLAIN, 11));
			setBackground(Color.white);
			Border border;
			if (Util.sex(miniCard.person) == 2)
				border = BorderFactory.createLineBorder(Color.pink, 1);
			else
				border = BorderFactory.createLineBorder(Color.blue, 1);
			setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createLineBorder(Color.white, 5)));
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
			setBounds(shiftX, shiftY, graph.width, graph.height);
			setBorder(BorderFactory.createLineBorder(Color.green, 1));
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(Color.lightGray);
			for (Line line : graph.getLines()) {
				int x1 = line.x1;
				int y1 = line.y1;
				int x2 = line.x2;
				int y2 = line.y2;
				// g.drawLine(x1, y1, x2, y2);
				Graphics2D g2 = (Graphics2D) g;
				CubicCurve2D c = new CubicCurve2D.Double();
				c.setCurve(x1, y1, x1, y2, x2, y1, x2, y2);
				g2.draw(c);
			}
		}
	}
}