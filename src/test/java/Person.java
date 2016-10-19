import java.time.Instant;

/**
 * Created by probakowski on 2016-10-19.
 */
public class Person
{
    private String name;
    private Instant createdAt;

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public Instant getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt( Instant createdAt )
    {
        this.createdAt = createdAt;
    }
}
