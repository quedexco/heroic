package com.spotify.heroic.grammar;

import org.mockito.Mockito;

import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public abstract class ExpressionTests {
    static <E extends Expression> void biFuncTest(
        Function<Context, E> a, Function<Context, E> b, Function<Context, E> e,
        BiFunction<E, E, E> op
    ) {
        final Context r = Mockito.mock(Context.class);
        final Context ac = Mockito.mock(Context.class);
        final Context bc = Mockito.mock(Context.class);

        doReturn(r).when(ac).join(bc);

        final E expected = e.apply(r);
        final E result = op.apply(a.apply(ac), b.apply(bc));

        assertEquals(expected, result);
        verify(ac).join(bc);

        assertEquals(r, expected.getContext());
    }

    static <E extends Expression> void uniFuncTest(
        Function<Context, E> a, Function<Context, E> e, Function<E, E> op
    ) {
        final Context c = Mockito.mock(Context.class);

        final E expected = e.apply(c);
        final E result = op.apply(a.apply(c));

        assertEquals(expected, result);
        assertEquals(c, expected.getContext());
    }

    static <E extends Expression> void visitorTest(
        E expr, BiFunction<Expression.Visitor<Void>, E, Void> verify
    ) {
        final Expression.Visitor<Void> visitor = Mockito.mock(Expression.Visitor.class);
        expr.visit(visitor);
        verify.apply(verify(visitor), expr);
    }
}
