/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.ten.ware.stream.stream;

import java.util.Iterator;
import java.util.Spliterator;

/**
 * Base interface for streams, which are sequences of elements supporting
 * sequential and parallel aggregate operations.  The following example
 * illustrates an aggregate operation using the stream types {@link Stream}
 * and {@link IntStream}, computing the sum of the weights of the red widgets:
 *
 * <pre>{@code
 *     int sum = widgets.stream()
 *                      .filter(w -> w.getColor() == RED)
 *                      .mapToInt(w -> w.getWeight())
 *                      .sum();
 * }</pre>
 * <p>
 * See the class documentation for {@link Stream} and the package documentation
 * for <a href="package-summary.html">java.util.stream</a> for additional
 * specification of streams, stream operations, stream pipelines, and
 * parallelism, which governs the behavior of all stream types.
 * <p>
 * 流的基本接口，流是支持顺序和并行聚合操作的元素序列。下面的示例演示了使用stream和IntStream类型的聚合操作，计算红色小部件的权重总和:
 * 请参阅Stream的类文档和java.util的包文档。流用于流、流操作、流管道和并行性的附加规范，这些规范控制所有流类型的行为。
 *
 * @param <T> the type of the stream elements
 * @param <S> the type of the stream implementing {@code BaseStream}
 * @see Stream
 * @see IntStream
 * @see LongStream
 * @see DoubleStream
 * @see <a href="package-summary.html">java.util.stream</a>
 * @since 1.8
 */
public interface BaseStream<T, S extends BaseStream<T, S>>
        extends AutoCloseable {
    /**
     * Returns an iterator for the elements of this stream.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     * 返回此流元素的迭代器。这是一个终端操作。
     *
     * @return the element iterator for this stream
     */
    Iterator<T> iterator();

    /**
     * Returns a spliterator for the elements of this stream.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * <p>
     * The returned spliterator should report the set of characteristics derived
     * from the stream pipeline (namely the characteristics derived from the
     * stream source spliterator and the intermediate operations).
     * Implementations may report a sub-set of those characteristics.  For
     * example, it may be too expensive to compute the entire set for some or
     * all possible stream pipelines.
     * 返回此流元素的spliterator。这是一个终端操作。返回的spliterator应该报告来自流管道的一组特征(即来自流源spliterator和中间操作的特征)。
     * 实现可以报告这些特征的子集。例如，为一些或所有可能的流管道计算整个集合可能太昂贵。
     *
     * @return the element spliterator for this stream
     */
    Spliterator<T> spliterator();

    /**
     * Returns whether this stream, if a terminal operation were to be executed,
     * would execute in parallel.  Calling this method after invoking an
     * terminal stream operation method may yield unpredictable results.
     * 如果要执行终端操作，则返回此流是否将并行执行。在调用终端流操作方法后调用此方法可能会产生不可预知的结果。
     *
     * @return {@code true} if this stream would execute in parallel if executed
     */
    boolean isParallel();

    /**
     * Returns an equivalent stream that is sequential.  May return
     * itself, either because the stream was already sequential, or because
     * the underlying stream state was modified to be sequential.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     * 返回一个等价的连续流。可能返回本身，这可能是因为流已经是顺序的，也可能是因为底层流状态被修改为顺序的。这是一个中间操作。
     *
     * @return a sequential stream
     */
    S sequential();

    /**
     * Returns an equivalent stream that is parallel.  May return
     * itself, either because the stream was already parallel, or because
     * the underlying stream state was modified to be parallel.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     * 返回一个并行的等效流。可能返回本身，这可能是因为流已经是并行的，也可能是因为底层流状态被修改为并行的。这是一个中间操作。
     *
     * @return a parallel stream
     */
    S parallel();

    /**
     * Returns an equivalent stream that is
     * <a href="package-summary.html#Ordering">unordered</a>.  May return
     * itself, either because the stream was already unordered, or because
     * the underlying stream state was modified to be unordered.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     * 返回一个无序的等价流。可能返回本身，这可能是因为流已经是无序的，也可能是因为底层流状态被修改为无序的。这是一个中间操作。
     *
     * @return an unordered stream
     */
    S unordered();

    /**
     * Returns an equivalent stream with an additional close handler.  Close
     * handlers are run when the {@link #close()} method
     * is called on the stream, and are executed in the order they were
     * added.  All close handlers are run, even if earlier close handlers throw
     * exceptions.  If any close handler throws an exception, the first
     * exception thrown will be relayed to the caller of {@code close()}, with
     * any remaining exceptions added to that exception as suppressed exceptions
     * (unless one of the remaining exceptions is the same exception as the
     * first exception, since an exception cannot suppress itself.)  May
     * return itself.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     * 返回带有附加关闭处理程序的等效流。Close处理程序是在流上调用Close()方法时运行的，并按照添加它们的顺序执行。
     * 所有关闭处理程序都将运行，即使先前的关闭处理程序抛出异常。
     * 如果任何close处理程序抛出一个异常，抛出的第一个异常将被传递给close()的调用者，
     * 其余的异常将作为被抑制的异常添加到该异常中(除非其余异常之一与第一个异常相同，因为异常不能被抑制自身)。
     * 可能会返回本身。这是一个中间操作。
     *
     * @param closeHandler A task to execute when the stream is closed
     * @return a stream with a handler that is run if the stream is closed
     */
    S onClose(Runnable closeHandler);

    /**
     * Closes this stream, causing all close handlers for this stream pipeline
     * to be called.
     * 关闭此流，导致调用此流管道的所有关闭处理程序。
     *
     * @see AutoCloseable#close()
     */
    @Override
    void close();
}
