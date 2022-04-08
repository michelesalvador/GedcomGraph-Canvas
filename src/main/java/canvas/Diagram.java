package canvas;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.CubicCurve2D;
import java.io.File;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.OverlayLayout;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;
import graph.gedcom.PersonNode;
import graph.gedcom.Bond;
import graph.gedcom.CurveLine;
import graph.gedcom.FamilyNode;
import graph.gedcom.Metric;
import graph.gedcom.Graph;
import graph.gedcom.Util;
import graph.gedcom.Line;
import static graph.gedcom.Util.*;

public class Diagram {

	Graph graph;
	Person fulcrum;
	Person firstFulcrum;
	JScrollPane scrollPane;
	Box box;
	Component lines;
	Component backLines; // Dashed lines of multi partners
	static int shiftX = 0;
	static int shiftY = 0;
	private Timer timer;
	float scale = 1;
	ComponentOrientation orient = ComponentOrientation.RIGHT_TO_LEFT; // LEFT_TO_RIGHT;

	Diagram() throws Exception {

		/* Redefine some spacing constants
		HORIZONTAL_SPACE = 10;
		BOND_WIDTH = 15;
		MINI_BOND_WIDTH = 10;
		MARRIAGE_WIDTH = 30;
		MARRIAGE_HEIGHT = 20;
		*/

		// Swing stuff
		JFrame frame = new JFrame();
		//frame.setSize(Toolkit.getDefaultToolkit().getScreenSize());
		frame.setSize(new Dimension(1500,700));
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

		// A slider to scale the diagram
		JSlider scaleSlider = new JSlider(1, 100, 100);
		header.add(scaleSlider);
		scaleSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if( !scaleSlider.getValueIsAdjusting() ) {
					scale = scaleSlider.getValue() / 100f;
					box.setPreferredSize(new Dimension( // Only to resize the scrollbars
							(int)((graph.getWidth() + shiftX * 2) * scale), (int)((graph.getHeight() + shiftY * 2) * scale)));
					scrollPane.getViewport().revalidate(); // Redraws box and scrollbars
					box.repaint(); // Whether box is smaller than scrollPane
				}
			}
		});

		box = new Box();
		scrollPane = new JScrollPane(box);
		frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
		frame.setVisible(true);

		// Parse a Gedcom file
		/*File file = new File("src/main/resources/tree2.ged");
		Gedcom gedcom = new ModelParser().parseGedcom(file);
		gedcom.createIndexes();*/

		// Directly open a JSON file
		String content = FileUtils.readFileToString(new File("src/main/resources/tree2.json"), "UTF-8");
		Gedcom gedcom = new JsonParser().fromJson(content);

		// Create the diagram model from the Gedcom object
		graph = new Graph(gedcom);
		graph.showFamily(0).maxAncestors(5).maxGreatUncles(5).displaySpouses(true).maxDescendants(5).maxSiblingsNephews(5).maxUnclesCousins(5);
		fulcrum = gedcom.getPerson("I1");
		firstFulcrum = fulcrum;

		timer = new Timer(40, e -> {  // 40 milliseconds = 25 fps
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

	class Box extends JPanel {
		Box() {
			setBackground(Color.darkGray);
			setOpaque(true);
		}
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			g2.scale(scale, scale);
		}
	}

	// Initialize the diagram the first time and clicking on a card
	private void startDiagram() {

		// Build the diagram of this person
		graph.startFrom(fulcrum);

		// Place the nodes on the canvas without position
		box.setLayout(new OverlayLayout(box)); // This layout let the nodes auto-size
		for( PersonNode personNode : graph.getPersonNodes() ) {
			if( personNode.person.equals(fulcrum) && !personNode.isFulcrumNode() )
				box.add(new Asterisk(personNode));
			else if( personNode.mini )
				box.add(new GraphicMiniCard(personNode));
			else
				box.add(new GraphicPerson(personNode));
		}
		box.validate(); // To calculate the dimensions of child componenets

		// Get the dimensions of each node
		for( Component compoNode : box.getComponents() ) {
			Metric metric = ((GraphicMetric)compoNode).metric;
			metric.width = compoNode.getWidth();
			metric.height = compoNode.getHeight();
		}

		// Let the diagram initialize nodes and lines
		graph.initNodes();

		box.setLayout(null); // This non-layout let the nodes in absolute position

		// Add marriage bonds
		graph.getBonds().forEach(bond -> {
			box.add(new GraphicBond(bond), 0);
		});

		if( graph.needMaxBitmap() )
			graph.setMaxBitmap(3000, 3000); // In Android these values come from canvas.getMaximumBitmapWidth() and canvas.getMaximumBitmapHeight()

		// First raw calculation of nodes position
		graph.placeNodes();

		// Add the lines
		lines = box.add(new GraphicLines(graph.getLines(), new BasicStroke(2)));
		backLines = box.add(new GraphicLines(graph.getBackLines(), new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0)));

		// First visible displacement of the diagram
		displaceDiagram();
		box.repaint(); // Clears dirty

		//timer.start();
	}

	// Visible position of nodes and lines
	void displaceDiagram() {
		for( Component compoNode : box.getComponents() ) {
			if( compoNode instanceof GraphicMetric ) {
				Metric metric = ((GraphicMetric)compoNode).metric;
				compoNode.setLocation((int)(orient.isLeftToRight() ? metric.x + shiftX
						: graph.getWidth() - metric.x - metric.width + shiftX), (int)metric.y + shiftY);
			}
		}
		lines.setBounds(shiftX, shiftY, (int)graph.getWidth(), (int)graph.getHeight());
		lines.repaint();
		backLines.setBounds(shiftX, shiftY, (int)graph.getWidth(), (int)graph.getHeight());
		backLines.repaint();
		box.setPreferredSize(new Dimension(
				(int)((graph.getWidth() + shiftX * 2) * scale), (int)((graph.getHeight() + shiftY * 2) * scale)));
		scrollPane.validate(); // Update scrollbars
	}

	// Graphical rappresentation of a single node
	abstract class GraphicMetric extends JLabel {
		Metric metric;
		GraphicMetric(Metric metric) {
			this.metric = metric;
			//setBorder(BorderFactory.createLineBorder(Color.cyan, 1));
			setLayout(new OverlayLayout(this)); // Admit overlapping of components
			setFont(new Font("Segoe UI", Font.PLAIN, 11));
			setOpaque(true);
		}
	}

	// Graphical realization of an individual card
	class GraphicPerson extends GraphicMetric {
		GraphicPerson(PersonNode node) {
			super(node);
			setText(node.toString());
			Color backgroundColor = Color.white;
			if( node.person.equals(fulcrum) ) {
				backgroundColor = Color.orange;
			} else if( node.acquired ) {
				backgroundColor = new Color(0xCCCCCC);
			}
			setBackground(backgroundColor);
			Color borderColor = Color.gray;
			Gender gender = Gender.getGender(node.person);
			if( gender == Gender.MALE )
				borderColor = Color.blue;
			else if( gender == Gender.FEMALE )
				borderColor = Color.pink;
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
			if( ((PersonNode)metric).dead ) {
				int[] pX = { (int)metric.width - 12, (int)metric.width - 7, (int)metric.width, (int)metric.width };
				int[] pY = { 0, 0, 7, 12 };
				g.fillPolygon(pX, pY, 4);
			}
		}
	}

	// Replacement for any person who is actually fulcrum
	class Asterisk extends GraphicMetric {
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

	class GraphicBond extends GraphicMetric {
		Bond bond;
		GraphicBond(Bond bond) {
			super(bond);
			this.bond = bond;
			//setBorder(BorderFactory.createLineBorder(Color.yellow, 1));
			setSize((int)bond.width, (int)bond.height);
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					JOptionPane.showMessageDialog(null, "Family " + bond.familyNode.spouseFamily.getId());
				}
			});
		}
		@Override
		protected void paintComponent(Graphics g) {
			FamilyNode familyNode = bond.familyNode;
			// Draw the marriage
			if( bond.marriageDate != null ) {
				int y = (int)bond.centerRelY() - MARRIAGE_HEIGHT / 2;
				g.setColor(new Color(0xFFFFFF));
				g.fillOval(0, y, MARRIAGE_WIDTH, MARRIAGE_HEIGHT);
				g.setColor(new Color(0xAAAAAA));
				g.drawOval(0, y, MARRIAGE_WIDTH, MARRIAGE_HEIGHT);
				g.setColor(Color.black);
				g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
				g.drawString(bond.marriageYear(), 0, y + 13);
			} else {
				g.setColor(Color.lightGray);
				int diameter = HEARTH_DIAMETER;
				if( familyNode.mini )
					diameter = MINI_HEARTH_DIAMETER;
				g.fillOval((int)bond.centerRelX() - diameter / 2, (int)bond.centerRelY() - diameter / 2, diameter, diameter);
			}
		}
	}

	class GraphicMiniCard extends GraphicMetric {
		GraphicMiniCard(PersonNode node) {
			super(node);
			setText(node.amount > 100 ? "100+" : String.valueOf(node.amount));
			Color backgroundColor = node.acquired ? new Color(0xCCCCCC) : Color.white;
			setBackground(backgroundColor);
			Color borderColor = Color.gray;
			Gender gender = Gender.getGender(node.person);
			if( gender == Gender.MALE )
				borderColor = Color.blue;
			else if( gender == Gender.FEMALE )
				borderColor = Color.pink;
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor, 1),
					BorderFactory.createLineBorder(backgroundColor, 4)));
			addMouseListener(new MouseAdapter() {
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
		List<Set<Line>> lineGroups;
		Stroke stroke;
		Color[] colors = { Color.lightGray, Color.RED, Color.CYAN, Color.MAGENTA, Color.GREEN, Color.PINK, Color.BLACK, Color.YELLOW, Color.BLUE, Color.ORANGE };	
		GraphicLines(List<Set<Line>> lineGroups, Stroke stroke) {
			this.lineGroups = lineGroups;
			this.stroke = stroke;
			setBounds(shiftX, shiftY, (int)graph.getWidth(), (int)graph.getHeight());
			setBorder(BorderFactory.createLineBorder(Color.orange, 1));
			setOpaque(false);
		}
		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			int width = (int)graph.getWidth();
			int i = 0;
			for( Set<Line> group : lineGroups ) {
				g.setColor(colors[i % colors.length]);
				for( Line line : group ) {
					int x1 = (int)line.x1;
					int y1 = (int)line.y1;
					int x2 = (int)line.x2;
					int y2 = (int)line.y2;
					//g.setColor(Color.lightGray);
					g2.setStroke(stroke);
					if( line instanceof CurveLine ) {
						CubicCurve2D cc = new CubicCurve2D.Double();
						if(orient.isLeftToRight())
							cc.setCurve(x1, y1, x1, y2 , x2, y1, x2, y2);
						else
							cc.setCurve(width - x1, y1, width - x1, y2, width - x2, y1, width - x2, y2);
						g2.draw(cc);
					} else { // Horizontal or vertical line
						if(orient.isLeftToRight())
							g.drawLine(x1, y1, x2, y2);
						else
							g.drawLine(width - x1, y1, width - x2, y2);
					}
				}
				i++;
			}
			// Rectangle to see the size of one group of lines
			g.setColor(Color.GRAY);
			g.drawRect(0, 0, graph.getMaxBitmapWidth(), graph.getMaxBitmapHeight());
		}
	}
}
