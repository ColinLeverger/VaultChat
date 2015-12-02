/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractListModel;

/**
 * Classe qui permet de trier les elements d'une JList
 * 
 * @author Gwenole Lecorve
 * @author David Guennec
 */
class SortedListModel extends AbstractListModel
{

	/**
	 *
	 */
	private static final long serialVersionUID = -8004106647341261479L;
	SortedSet model;

	public SortedListModel()
	{
		this.model = new TreeSet();
	}

	public void add(Object element)
	{
		if ( this.model.add(element) ) {
			fireContentsChanged(this, 0, getSize());
		}
	}

	public void addAll(Object elements[])
	{
		Collection c = Arrays.asList(elements);
		this.model.addAll(c);
		fireContentsChanged(this, 0, getSize());
	}

	public void clear()
	{
		this.model.clear();
		fireContentsChanged(this, 0, getSize());
	}

	public boolean contains(Object element)
	{
		return this.model.contains(element);
	}

	public Object firstElement()
	{
		return this.model.first();
	}

	@Override
	public Object getElementAt(int index)
	{
		return this.model.toArray()[index];
	}

	public int getElementIndex(Object o) throws Exception
	{
		int index = -1;
		for ( Iterator it = this.model.iterator(); it.hasNext(); ) {
			index++;
			if ( it.next().equals(o) ) {
				return index;
			}
		}
		throw new Exception("Object has not been found in the the sorted list model.");
	}

	@Override
	public int getSize()
	{
		return this.model.size();
	}

	public Iterator iterator()
	{
		return this.model.iterator();
	}

	public Object lastElement()
	{
		return this.model.last();
	}

	public boolean removeElement(Object element)
	{
		boolean removed = this.model.remove(element);
		if ( removed ) {
			fireContentsChanged(this, 0, getSize());
		}
		return removed;
	}
}
