import java.time.Instant;

/**
 * Created by probakowski on 2016-10-19.
 */
public class SerializedOperation<T, E>
{
    private Instant clock;
    private Operation<T, E> operation;

    //For Kryo
    private SerializedOperation()
    {

    }

    public SerializedOperation( Instant clock, Operation<T, E> operation )
    {
        this.clock = clock;
        this.operation = operation;
    }

    public Instant getClock()
    {
        return clock;
    }

    public Operation<T, E> getOperation()
    {
        return operation;
    }
}
