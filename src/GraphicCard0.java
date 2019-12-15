import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import org.folg.gedcom.model.Person;
import graph.gedcom.Card;
import graph.gedcom.Util;

public class GraphicCard0 extends Card implements MouseListener { // extends LinearLayout
	
	public boolean dead;
	
	public GraphicCard0(Person person) {
		super(person);
		//Util.p(Util.essence(person));
		if(Util.dead(person)) {
			dead = true;
		}
		//Component comp = new java.awt.Component();
		//addMouseListener(new CardClick());
	}

	@Override
	public void mouseClicked(MouseEvent event) {
		Util.p(event.getSource());
	}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseReleased(MouseEvent e) {}
	
	public class CardClick extends MouseAdapter {

		/*public CardClick(GraphicCard0 card) {
			card.addMouseListener(card);
		}*/

	    public void mouseClicked(MouseEvent event) {
			Util.p(event.getSource());
	    }
	}
}