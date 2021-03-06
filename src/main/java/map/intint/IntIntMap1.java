package map.intint;

/**
 * The original version of int-int map.
 * Has a lot of problems which will be fixed in the following versions.
 */
public class IntIntMap1 implements IntIntMap
{
    public static final int NO_KEY = 0;
    public static final int NO_VALUE = 0;

    /** Keys */
    private int[] m_keys;
    /** Values */
    private int[] m_values;
    /** Occupied? */
    private boolean[] m_used;

    /** Fill factor, must be between (0 and 1) */
    private final float m_fillFactor;
    /** We will resize a map once it reaches this size */
    private int m_threshold;
    /** Current map size */
    private int m_size;

    public IntIntMap1( final int size, final float fillFactor )
    {
        if ( fillFactor <= 0 || fillFactor >= 1 )
            throw new IllegalArgumentException( "FillFactor must be in (0, 1)" );
        if ( size <= 0 )
            throw new IllegalArgumentException( "Size must be positive!" );
        final long capacity = (long) (size / fillFactor + 1);
        if ( capacity > Integer.MAX_VALUE )
            throw new IllegalArgumentException( "Too large map capacity requested! Requested = " + capacity +
                    "; max possible = " + Integer.MAX_VALUE );
        m_keys = new int[(int) capacity];
        m_values = new int[(int) capacity];
        m_used = new boolean[(int) capacity];
        m_threshold = size;
        m_fillFactor = fillFactor;
    }

    public synchronized int get( final int key )
    {
        final int idx = getReadIndex( key );
        return idx != -1 ? m_values[ idx ] : NO_VALUE;
    }

    public synchronized int put( final int key, final int value )
    {
        int idx = getPutIndex( key );
        if ( idx < 0 )
        { //no insertion point? Should not happen...
            rehash( m_keys.length * 2 );
            idx = getPutIndex( key );
        }
        final int prev = m_values[ idx ];
        if ( !m_used[ idx ] )
        {
            m_keys[ idx ] = key;
            m_values[ idx ] = value;
            m_used[ idx ] = true;
            ++m_size;
            if ( m_size >= m_threshold )
                rehash( m_keys.length * 2 );
        }
        else //it means used cell with our key
        {
            assert m_keys[ idx ] == key;
            m_values[ idx ] = value;
        }
        return prev;
    }

    public synchronized int remove( final int key )
    {
        int idx = getReadIndex( key );
        if ( idx == -1 )
            return NO_VALUE;
        final int res = m_values[ idx ];
        shiftKeys( idx );
        --m_size;
        return res;
    }

    public synchronized int size()
    {
        return m_size;
    }

    private void rehash( final int newCapacity )
    {
        m_threshold = (int) (newCapacity * m_fillFactor);
        final int oldCapacity = m_keys.length;
        final int[] oldKeys = m_keys;
        final int[] oldValues = m_values;
        final boolean[] oldStates = m_used;

        m_keys = new int[ newCapacity ];
        m_values = new int[ newCapacity ];
        m_used = new boolean[ newCapacity ];
        m_size = 0;

        for ( int i = oldCapacity; i-- > 0; ) {
            if( oldStates[i] )
                put( oldKeys[ i ], oldValues[ i ] );
        }
    }

    /**
     * Find key position in the map.
     * @param key Key to look for
     * @return Key position or -1 if not found
     */
    private int getReadIndex( final int key )
    {
        int idx = getStartIndex( key );
        if ( m_keys[ idx ] == key && m_used[ idx ] )
            return idx;
        if ( !m_used[ idx ] ) //end of chain already
            return -1;
        final int startIdx = idx;
        while (( idx = getNextIndex( idx ) ) != startIdx )
        {
            if ( !m_used[ idx ] )
                return -1;
            if ( m_keys[ idx ] == key && m_used[ idx ] )
                return idx;
        }
        return -1;
    }

    /**
     * Find an index of a cell which should be updated by 'put' operation.
     * It can be:
     * 1) a cell with a given key
     * 2) first free cell in the chain
     * @param key Key to look for
     * @return Index of a cell to be updated by a 'put' operation
     */
    private int getPutIndex( final int key )
    {
        final int readIdx = getReadIndex( key );
        if ( readIdx >= 0 )
            return readIdx;
        //key not found, find insertion point
        final int startIdx = getStartIndex( key );
        int idx = startIdx;
        while ( m_used[ idx ] )
        {
            idx = getNextIndex( idx );
            if ( idx == startIdx )
                return -1;
        }
        return idx;
    }


    private int getStartIndex( final int key )
    {
        final int idx = Tools.phiMix( key ) % m_keys.length;
        return idx >= 0 ? idx : -idx;
    }

    private int getNextIndex( final int currentIndex )
    {
        return currentIndex < m_keys.length - 1 ? currentIndex + 1 : 0;
    }

    private int shiftKeys(int pos)
    {
        // Shift entries with the same hash.
        int last, slot;
        int k;
        final int[] keys = this.m_keys;
        while ( true )
        {
            last = pos;
            pos = getNextIndex(pos);
            while ( true )
            {
                k = keys[ pos ];
                if ( !m_used[pos] )
                {
                    keys[last] = NO_KEY;
                    m_values[ last ] = NO_VALUE;
                    m_used[ last ] = false;
                    return last;
                }
                slot = getStartIndex(k); //calculate the starting slot for the current key
                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                pos = getNextIndex(pos);
            }
            keys[last] = k;
            m_values[last] = m_values[pos];
            m_used[last] = m_used[pos];
        }
    }

}
