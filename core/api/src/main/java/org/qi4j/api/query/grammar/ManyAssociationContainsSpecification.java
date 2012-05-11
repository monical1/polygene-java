package org.qi4j.api.query.grammar;

import org.qi4j.api.association.ManyAssociation;
import org.qi4j.api.composite.Composite;

/**
 * TODO
 */
public class ManyAssociationContainsSpecification<T>
    extends ExpressionSpecification
{
    private ManyAssociationFunction<T> manyAssociationFunction;
    private T value;

    public ManyAssociationContainsSpecification( ManyAssociationFunction<T> manyAssociationFunction, T value )
    {
        this.manyAssociationFunction = manyAssociationFunction;
        this.value = value;
    }

    public ManyAssociationFunction<T> getManyAssociationFunction()
    {
        return manyAssociationFunction;
    }

    public T getValue()
    {
        return value;
    }

    @Override
    public boolean satisfiedBy( Composite item )
    {
        ManyAssociation<T> collection = manyAssociationFunction.map( item );
        if( collection == null )
        {
            return false;
        }
        return collection.contains( value );
    }

    @Override
    public String toString()
    {
        return manyAssociationFunction + " contains:" + value;
    }
}