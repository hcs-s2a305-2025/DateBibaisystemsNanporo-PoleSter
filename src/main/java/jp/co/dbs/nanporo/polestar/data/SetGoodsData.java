package jp.co.dbs.nanporo.polestar.data;

public class SetGoodsData {
    
    // セット商品ID
    private Integer setGoodsId;
    
    // セット商品名
    private String setName; 
    
    // セット商品の加算料金
    private Integer setPrice;   
    
    // カロリー
    private Integer calorie;    
    
    // アレルゲン（String等に変更が必要な場合は調整）
    private Integer allergy;    
    
    // 売り切れフラグ
    private Boolean soldOut;    

    // コンストラクタ（空）
    public SetGoodsData() {}

    // Getter / Setter
    public Integer getSetGoodsId() { return setGoodsId; }
    public void setSetGoodsId(Integer setGoodsId) { this.setGoodsId = setGoodsId; }

    public String getSetName() { return setName; }
    public void setSetName(String setName) { this.setName = setName; }

    public Integer getSetPrice() { return setPrice; }
    public void setSetPrice(Integer setPrice) { this.setPrice = setPrice; }

    public Integer getCalorie() { return calorie; }
    public void setCalorie(Integer calorie) { this.calorie = calorie; }

    public Integer getAllergy() { return allergy; }
    public void setAllergy(Integer allergy) { this.allergy = allergy; }

    public Boolean getSoldOut() { return soldOut; }
    public void setSoldOut(Boolean soldOut) { this.soldOut = soldOut; }
}