package pl.robakowski.omni.jmh.db.data;

import javax.persistence.*;

@Table(name = "pet", indexes = {@Index(name = "name", columnList = "name")})
@Entity
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne
    @JoinColumn(name = "owner")
    private Person owner;

    @Column
    private String name;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Person getOwner() {
        return owner;
    }

    public void setOwner(Person owner) {
        if (owner != this.owner) {
            if (this.owner != null)
                this.owner.getPets().remove(this);
            this.owner = owner;
            owner.getPets().add(this);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
