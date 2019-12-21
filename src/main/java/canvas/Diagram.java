package canvas;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.CubicCurve2D;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;
import graph.gedcom.Card;
import graph.gedcom.Graph;
import graph.gedcom.Node;
import graph.gedcom.Util;
import graph.gedcom.Line;
import graph.gedcom.Couple;
import static graph.gedcom.Util.pr;

public class Diagram {
	
	JFrame frame;
	Graph graph;
	
	static int sizeHoriz = 1600;
	static int sizeVert = 1000;
	static int shiftX = 10;
	static int shiftY = 10;
		
	Diagram () throws Exception {
		
		// Create the JFrame
		frame = new JFrame();		
		frame.setSize(sizeHoriz, sizeVert);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(null);
		frame.setVisible(true);
		
		// Parse a Gedcom file
		File file = new File("src/main/resources/family.ged");	
		Gedcom gedcom = new ModelParser().parseGedcom(file);
		gedcom.createIndexes();
		
		// Directly open a Json file
		//String content = FileUtils.readFileToString(new File("..\\esempi\\famiglia.ged.json"), "UTF-8");
		//Gedcom gedcom = new JsonParser().fromJson(content);

		// Create the diagram model from the Gedcom object
		graph = new Graph(gedcom);
		graph.showFamily(0).maxAncestors(2).startFrom("I1");	// .setCardClass(GraphicCard.class)
		//pr(graph.toString());

		paintDiagram();		
	}
	
	public static void main(String[] args) throws Exception {
		new Diagram();
	}

	private void paintDiagram() {

		// Pass the dimensions of each card
		for ( Card card : graph.getCards()) {
			card.width = Util.essence(card.getPerson()).length() * 5 + 15;
			card.height = (int) (25 + (40 * Math.random()));
		}
		
		// Let the diagram calculate positions of Nodes and Lines
		graph.arrange();

		// Place the nodes that place the cards on the canvas
		for(Node node : graph.getNodes()) {
			new GraphicNode(node);
		}
		
		// Draw the lines
		JPanel linesPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.setColor(Color.gray);
				for(Line line : graph.getLines()) {
					int x1 = line.x1+shiftX;
					int y1 = line.y1+shiftY;
					int x2 = line.x2+shiftX;
					int y2 = line.y2+shiftY;
					//g.drawLine(x1, y1, x2, y2);
					Graphics2D g2 = (Graphics2D) g;
					CubicCurve2D c = new CubicCurve2D.Double();
					c.setCurve(x1, y1, x1, y2,  x2, y1, x2, y2);
					g2.draw(c);
				}
				
				// Cartesian center
				g.setColor(Color.green);
				g.drawLine(-50+shiftX, shiftY, shiftX+100, shiftY);
				g.drawLine(shiftX, -50+shiftY, shiftX, shiftY+100);
			}
		};
		linesPanel.setBounds(0, 0, sizeHoriz, sizeVert);
		linesPanel.setOpaque(false);
		frame.getContentPane().add(linesPanel);
		
		//pr(graph.toString());
		frame.getContentPane().repaint();
	}

	// Graphical rappresentation of a node
	class GraphicNode extends JPanel {

		Node node;

		GraphicNode(Node node) {
			this.node = node;
			setBounds(node.x+shiftX, node.y+shiftY, node.width, node.height);
			//setBorder(BorderFactory.createLineBorder(Color.cyan, 1));
			setOpaque(false);
			frame.getContentPane().add(this);
			
			// Create the cards
			if(node.isCouple()) {
				new GraphicCard(((Couple)node).husband);
				new GraphicCard(((Couple)node).wife);
			} else
				new GraphicCard(node.getMainCard());
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if(node.isCouple()) {
				Couple couple = (Couple) node;
				// Draw the vertical line from marriage
				if(couple.guardGroup != null && !couple.guardGroup.getYouths().isEmpty()) {
					g.setColor(Color.gray);
					g.drawLine(couple.centerXrel(), couple.centerYrel(), couple.centerXrel(), couple.height);
				}
				// Draw the marriages
				if(couple.marriageDate != null ) {
					int w = 25;
					int h = 17;
					int x = couple.centerXrel() - w/2;
					int y = couple.centerYrel() - h/2;
					g.setColor(Color.lightGray);
					g.fillOval(x, y, w, h);
					g.setColor(Color.black);
					g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
					g.drawString(couple.marriageYear(), x, y + 12);
				} else {
					// Draw a simple line
					g.setColor(Color.gray);
					g.drawLine(couple.husband.width, couple.centerYrel(), couple.husband.width + Util.MARGIN, couple.centerYrel());
				}
			}
		}
	}
	
	// Graphical realization of a card
	class GraphicCard extends JButton {
		
		Card card;
		GraphicCard(Card card) {
			super(Util.essence(card.getPerson()));
			this.card = card;
			setFont(new Font("Segoe UI", Font.PLAIN, 11));
			setBounds(card.x+shiftX, card.y+shiftY, card.width, card.height);
			//panel.setLayout(null);
			//panel.setLayout(new BorderLayout());
			if(card.getPerson().getId().equals(graph.getStartId()))
				setBackground(Color.orange);
			else
				setBackground(Color.white);
			Border border = BorderFactory.createLineBorder(Color.gray, 1);
			if( Util.sex(card.getPerson()) == 1 ) {
				border = BorderFactory.createLineBorder(Color.blue, 1);
			} else if( Util.sex(card.getPerson()) == 2 ) {
				border = BorderFactory.createLineBorder(Color.pink, 1);
			}
			setBorder(border);
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					graph.restartFrom(card.getPerson().getId());
					frame.getContentPane().removeAll();
					paintDiagram();
				}
			});
			frame.getContentPane().add(this);
		}
		
		@Override
		protected void paintComponent(Graphics g){
			super.paintComponent(g);
			// Death ribbon
			g.setColor(Color.black);
			if(card.dead) {
				int[] pX = { card.width-10, card.width-6, card.width, card.width };
				int[] pY = { 0, 0, 6, 10 };
				g.fillPolygon( pX, pY, 4 );
			}
		}
	}
}