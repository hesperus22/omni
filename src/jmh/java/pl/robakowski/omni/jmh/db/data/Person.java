package pl.robakowski.omni.jmh.db.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Table( name = "person" )
@Entity
public class Person
{
    @Id
    @GeneratedValue( strategy = GenerationType.AUTO )
    private long id;

    @Column( name = "age" )
    private int age;

    public long getId()
    {
        return id;
    }

    public void setId( long id )
    {
        this.id = id;
    }

    public int getAge()
    {
        return age;
    }

    public void setAge( int age )
    {
        this.age = age;
    }
}
