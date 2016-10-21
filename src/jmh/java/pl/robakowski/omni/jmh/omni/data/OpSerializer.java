package pl.robakowski.omni.jmh.omni.data;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Created by probakowski on 2016-10-21.
 */
class OpSerializer extends Serializer
{
    public OpSerializer()
    {
        setAcceptsNull( true );
    }

    @Override
    public void write( Kryo kryo, Output output, Object object )
    {

    }

    @Override
    public Object read( Kryo kryo, Input input, Class type )
    {
        return null;
    }
}
