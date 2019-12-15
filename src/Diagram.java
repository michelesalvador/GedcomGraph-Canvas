import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.parser.JsonParser;
import graph.gedcom.Card;
import graph.gedcom.Graph;
import graph.gedcom.Node;
import graph.gedcom.Util;
import graph.gedcom.Line;
import graph.gedcom.Couple;
import static graph.gedcom.Util.print;

public class Diagram {
	
	JFrame frame;
	Graph graph;
	
	static int sizeHoriz = 1600;
	static int sizeVert = 1000;
	static int shiftX = 300;
	static int shiftY = 100;
		
	Diagram () throws IOException {
		
		// Create the JFrame
		frame = new JFrame() {
			@Override
			public void paint(Graphics g) {
				// Place the Nodes and the Lines on the canvas
				/*for (Node node : graph.getNodes()) {
					if(node.isCouple()) {
						if(((Couple)node).marriage != null )
							drawMarriage( g, (Couple)node );
					}
					drawNode(g, node);
				}*/

				
				/* OK questo funziona
				JButton b = new JButton("push me");
				b.setBounds(10, 10, 120, 30);
				b.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						System.out.println(e);
					}
				});
				add(b);*/
			}
		};
		
		frame.setSize(sizeHoriz, sizeVert);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(null);
		//frame.setLayout(new BorderLayout(100,100));
		//frame.setBackground(Color.blue); // ok
		frame.setVisible(true);
		
		/* questo qui non funziona
		area.setOpaque(true);
		area.setBackground(Color.magenta);
		area.setBounds(10,10,500, 50);
		//area.setLayout(new BorderLayout(10, 10));
		area.setLayout(null);
	    //area.setPreferredSize(new Dimension(200, 200));
		frame.add(area);
		Util.p(area.getBackground());*/
		
		// Open the gedcom file
		String content = FileUtils.readFileToString(new File("..\\esempi\\famiglia.ged" + ".json"), "UTF-8"); // "src/test/resources/family.json"
		Gedcom gedcom = new JsonParser().fromJson(content);

		// Create the diagram model from the Gedcom object
		graph = new Graph(gedcom);
		graph.showFamily(0).setCardClass(GraphicCard.class).maxAncestors(2).startFrom("I825");
		//System.out.println(graph.toString());

		build();		
	}
	
	public static void main(String[] args) throws IOException {
		new Diagram();
	}

	//@Override
	private void build() {

		// Pass the dimensions of each card
		for ( Card card : graph.getCards()) {
			GraphicCard graphicCard = (GraphicCard)card;
			graphicCard.width = Util.essence(graphicCard.getPerson()).length() * 6 + 20;
			graphicCard.height = (int) (25 + (80 * Math.random()));
		}
		
		// Let the diagram calculate positions of Nodes and Lines
		graph.arrange();

		// Place the cards on the canvas
		for(Card card : graph.getCards()) {
			GraphicCard graphicCard = (GraphicCard)card;
			graphicCard.place(this);
		}
		
		// Place the lines
		new GraphicLines(this);
		
		/* ok
		JButton b = new JButton("push me 2");
		b.setBounds(400, 400, 250, 50);
		b.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println(e);
			}
		});
		frame.add(b);*/
		
		//frame.getContentPane().validate();
		frame.getContentPane().repaint();
	}

	void drawNode(Graphics g, Node node ) {
		g.setColor(Color.cyan);
		g.drawRect(node.x+shiftX, node.y+shiftY, node.width, node.height);
	}

	void drawMarriage(Graphics g, Couple couple ) {
		int h = 20;
		int x = couple.x + couple.husband.width - 5 + shiftX;
		int y = couple.y + couple.height/2 -h/2 + shiftY;
		g.setColor(Color.gray);
		g.drawLine(couple.centerX()+shiftX, couple.centerY()+shiftY, couple.centerX()+shiftX, couple.y+couple.height +shiftY);
		g.setColor(Color.lightGray);
		g.fillOval(x, y, Util.MARGIN + 10, h);
		g.setColor(Color.black);
		//g.setFont(new Font("Arial", Font.PLAIN, 5));
		g.drawString(couple.marriage, x + 5, y + 15);
	}
	
	// Graphic card with some other eventual attribute
	public static class GraphicCard extends Card {
		
		String id;
		public boolean dead;
		
		public GraphicCard(Person person) {
			super(person);
			id = person.getId();
			if(Util.dead(person)) {
				dead = true;
			}
		}
		
		public void place(Diagram diagram) {
			JButton panel = new JButton(Util.essence(getPerson())) {
	            @Override
	            protected void paintComponent(Graphics g){
	                super.paintComponent(g);
	                // Death ribbon
	                g.setColor(Color.black);
	    			if(dead) {
	    				int[] pX = { width-10, width-6, width, width };
	    				int[] pY = { 0, 0, 6, 10 };
	    				g.fillPolygon( pX, pY, 4 );
	    			}
	            }
	        };
	        panel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
			panel.setBounds(x+shiftX, y+shiftY, width, height);
			//panel.setLayout(null);
			//panel.setLayout(new BorderLayout());
			if(id.equals(diagram.graph.getStartId()))
				panel.setBackground(Color.orange);
			else
				panel.setBackground(Color.white);
			Border border = BorderFactory.createLineBorder(Color.gray, 1);
			if( Util.sex(getPerson()) == 1 ) {
				border = BorderFactory.createLineBorder(Color.blue, 1);
			} else if( Util.sex(getPerson()) == 2 ) {
				border = BorderFactory.createLineBorder(Color.pink, 1);
			}
			panel.setBorder(border);
			panel.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					//System.out.println(id);
					diagram.graph.restartFrom(id);
					diagram.frame.getContentPane().removeAll();
					diagram.build();
					//diagram.frame.getContentPane().validate();
					diagram.frame.getContentPane().repaint();
				}
			});
			
			/* Ok Questo compare sulla card...
			JButton button = new JButton(id);
			button.setBounds(0, 0, width, height);
			button.setOpaque(false);
			button.setForeground(Color.blue);
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String id = e.getActionCommand();
					System.out.println(id);
					//frame.graph.restartFrom(id);
					//frame.getContentPane().removeAll();
					//frame.validate();
					//frame.repaint();
				}
			});
			panel.add(button);*/
			
		    /*JPanel holdingPanel = new JPanel(new BorderLayout());
		    int eb = 20;
		    holdingPanel.setBorder(BorderFactory.createEmptyBorder(0, eb, eb, eb));
		    holdingPanel.add(button, BorderLayout.CENTER);
		    frame.add(holdingPanel, BorderLayout.CENTER);
			JButton button2 = new JButton("CAZZO");
			button2.setBorder(BorderFactory.createEmptyBorder(0,10,10,10));
			button2.setBounds(100,100,100,100);
			holdingPanel.add(button2);
			JButton button3 = new JButton("CAZZO 2");
			holdingPanel.add(button3);
			//button2.setBorder(redline);*/
			
			diagram.frame.getContentPane().add(panel);
			//panel.repaint();			
		}
	}
	
	static class GraphicLines {

		public GraphicLines(Diagram diagram) {
			JPanel panel = new JPanel() {
	            @Override
	            protected void paintComponent(Graphics g) {
	                super.paintComponent(g);
	        		//Util.p(line.x1+ "  "+line.y1+ "  "+ line.x2+ "  "+ line.y2);
	        		g.setColor(Color.gray);
	        		for(Line line : diagram.graph.getLines()) {
	        			g.drawLine(line.x1+shiftX, line.y1+shiftY, line.x2+shiftX, line.y2+shiftY);
	        		}
					
					// Cartesian center
					g.drawLine(-100+shiftX, shiftY, shiftX+100, shiftY);
					g.drawLine(shiftX, -100+shiftY, shiftX, shiftY+100);
	            }
			};
			panel.setBounds(0, 0, sizeHoriz, sizeVert);
			diagram.frame.getContentPane().add(panel);
		}		
	}
	
	/*static class ClickableArea extends Button implements ActionListener {
		private static final long serialVersionUID = 1L;
		@Override
		public void paint(Graphics g) {
		     super.paint(g);
		     Point point = getLocation();
		     g.drawRect(point.x-10, point.y-10, 20, 50);//drawing stomach
		     g.drawLine(point.x+10,point.y, point.x+20, point.y+50);//drawing right hand
		     g.drawLine(point.x-10, point.y, point.x-20, point.y+50);//drawing left hand
		     g.drawLine(point.x, point.y-10, point.x, point.y-20);//drawing neck
		     g.drawLine(point.x-5, point.y+40, point.x-5, point.y+100);//drawing left leg
		     g.drawLine(point.x+5, point.y+40, point.x+5, point.y+100);//drawing right leg
		     g.drawOval(point.x-10, point.y-40, 23, 20);//drawing head
		     g.drawRect(point.x-20, point.y-40, 40, 140);
		}
		@Override
		public void actionPerformed(ActionEvent event) {
			Util.p("perform "+event.getSource());
		}
	}
	
	static class CardClick extends MouseAdapter implements ActionListener  {
	    public void mouseClicked(MouseEvent event) {
			Util.p("Area "+event.getSource());
	    }
		@Override
		public void actionPerformed(ActionEvent event) {
			Util.p("perform "+event.getSource());
		}
	}*/
}