package com.example.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    @Column(name = "carId")
    private Integer carId;

    @Column(name = "ownerId", nullable = false)
    private Integer ownerId;

    @Column(name = "ownerName", length = 100)
    private String ownerName;

    @Column(name = "model", length = 100, nullable = false)
    private String model;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "releaseYear")
    private Integer releaseYear;

    @Column(name = "kms")
    private Integer kms;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "plate", length = 20)
    private String plate;

    @Column(name = "salesStatus", length = 30)
    private String salesStatus;

    @Column(name = "publicationDate")
    private LocalDateTime publicationDate;

    @Lob
    @Column(name = "imageData", columnDefinition = "LONGBLOB")
    private byte[] imageData;

    @Column(name = "imageType", length = 100)
    private String imageType;

    public Vehicle() {
    }

    public Integer getCarId() { return carId; }
    public void setCarId(Integer carId) { this.carId = carId; }

    public Integer getOwnerId() { return ownerId; }
    public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }

    public Integer getKms() { return kms; }
    public void setKms(Integer kms) { this.kms = kms; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getSalesStatus() { return salesStatus; }
    public void setSalesStatus(String salesStatus) { this.salesStatus = salesStatus; }

    public LocalDateTime getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDateTime publicationDate) { this.publicationDate = publicationDate; }

    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }

    public String getImageType() { return imageType; }
    public void setImageType(String imageType) { this.imageType = imageType; }
}