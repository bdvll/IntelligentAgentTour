package hw1;
import java.io.Serializable;

import hw1.Artifact;
public class User  implements Serializable {
	
	private int yearsOfInterest;
	private String genreInterest;
	private int age;
	private long id;

	public User(){
		this.yearsOfInterest = (int) (Math.random() * 600 + 1400);
		this.age = (int) Math.random()*100;
		this.genreInterest = Artifact.Genres[((int)Math.random()*(Artifact.Genres.length-1))];
		this.id = (long) Math.random()*System.currentTimeMillis();
	}

	public int getYearsOfInterest() {
		return yearsOfInterest;
	}

	public void setYearsOfInterest(int yearsOfInterest) {
		this.yearsOfInterest = yearsOfInterest;
	}

	public String getGenreInterest() {
		return genreInterest;
	}

	public void setGenreInterest(String genreInterest) {
		this.genreInterest = genreInterest;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
}
