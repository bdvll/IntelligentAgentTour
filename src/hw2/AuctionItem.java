package hw2;

import java.io.Serializable;
import java.util.List;

public class AuctionItem implements Serializable{
	private int itemName;
	private int price;
	private int lowestPrice;
	private String status;
	private int priceReductionAmount;
	private List<Long> artifactList;
	
	public AuctionItem(int itemName, int price, int lowestPrice) {
		super();
		this.itemName = itemName;
		this.price = price;
		this.lowestPrice = lowestPrice;
		this.priceReductionAmount = (int) price/10;
	}
	public List<Long> getArtifactList() {
		return artifactList;
	}
	public void setArtifactList(List<Long> artifactList) {
		this.artifactList = artifactList;
	}
	public int getItemName() {
		return itemName;
	}
	public void setItemName(int itemName) {
		this.itemName = itemName;
	}
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
		this.priceReductionAmount = (int) price/10;
	}
	public int getLowestPrice() {
		return lowestPrice;
	}
	public void setLowestPrice(int lowestPrice) {
		this.lowestPrice = lowestPrice;
	}
	public String getStatus(){
		return status;
	}
	public int getPriceReductionAmount() {
		return priceReductionAmount;
	}
	public void setPriceReductionAmount(int priceReductionAmount) {
		this.priceReductionAmount = priceReductionAmount;
	}
	public void setStatus(String s){
		this.status = s;
	}
	
	
}
