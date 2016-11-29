package hw3;

import java.io.Serializable;
import java.util.ArrayList;

public class SmarterArrayList<T> extends ArrayList<T> implements Serializable{

	@Override
	public T get(int index) {
		// TODO Auto-generated method stub
		T returnObj = null;
		try{
			returnObj = super.get(index);
		}catch(Exception e){
			returnObj = null;
		}
		return returnObj;
	}

	@Override
	public void add(int index, T element) {
		
		int size = super.size();
		while(size <= index -1){
			super.add(null);
		}
		super.add(index, element);
	}

	@Override
	public T set(int index, T element) {
		
		int size = super.size();
		while(size <= index -1){
			super.add(null);
		}
		return super.set(index, element);
	}
	

}
