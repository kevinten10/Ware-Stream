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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A <a href="package-summary.html#Reduction">mutable reduction operation</a> that
 * accumulates input elements into a mutable result container, optionally transforming
 * the accumulated result into a final representation after all input elements
 * have been processed.  Reduction operations can be performed either sequentially
 * or in parallel.
 *
 * <p>Examples of mutable reduction operations include:
 * accumulating elements into a {@code Collection}; concatenating
 * strings using a {@code StringBuilder}; computing summary information about
 * elements such as sum, min, max, or average; computing "pivot table" summaries
 * such as "maximum valued transaction by seller", etc.  The class {@link Collectors}
 * provides implementations of many common mutable reductions.
 *
 * <p>A {@code Collector} is specified by four functions that work together to
 * accumulate entries into a mutable result container, and optionally perform
 * a final transform on the result.  They are: <ul>
 * <li>creation of a new result container ({@link #supplier()})</li>
 * <li>incorporating a new data element into a result container ({@link #accumulator()})</li>
 * <li>combining two result containers into one ({@link #combiner()})</li>
 * <li>performing an optional final transform on the container ({@link #finisher()})</li>
 * </ul>
 *
 * <p>Collectors also have a set of characteristics, such as
 * {@link Characteristics#CONCURRENT}, that provide hints that can be used by a
 * reduction implementation to provide better performance.
 *
 * <p>A sequential implementation of a reduction using a collector would
 * create a single result container using the supplier function, and invoke the
 * accumulator function once for each input element.  A parallel implementation
 * would partition the input, create a result container for each partition,
 * accumulate the contents of each partition into a subresult for that partition,
 * and then use the combiner function to merge the subresults into a combined
 * result.
 *
 * <p>To ensure that sequential and parallel executions produce equivalent
 * results, the collector functions must satisfy an <em>identity</em> and an
 * <a href="package-summary.html#Associativity">associativity</a> constraints.
 *
 * <p>The identity constraint says that for any partially accumulated result,
 * combining it with an empty result container must produce an equivalent
 * result.  That is, for a partially accumulated result {@code a} that is the
 * result of any series of accumulator and combiner invocations, {@code a} must
 * be equivalent to {@code combiner.apply(a, supplier.get())}.
 *
 * <p>The associativity constraint says that splitting the computation must
 * produce an equivalent result.  That is, for any input elements {@code t1}
 * and {@code t2}, the results {@code r1} and {@code r2} in the computation
 * below must be equivalent:
 * <pre>{@code
 *     A a1 = supplier.get();
 *     accumulator.accept(a1, t1);
 *     accumulator.accept(a1, t2);
 *     R r1 = finisher.apply(a1);  // result without splitting
 *
 *     A a2 = supplier.get();
 *     accumulator.accept(a2, t1);
 *     A a3 = supplier.get();
 *     accumulator.accept(a3, t2);
 *     R r2 = finisher.apply(combiner.apply(a2, a3));  // result with splitting
 * } </pre>
 *
 * <p>For collectors that do not have the {@code UNORDERED} characteristic,
 * two accumulated results {@code a1} and {@code a2} are equivalent if
 * {@code finisher.apply(a1).equals(finisher.apply(a2))}.  For unordered
 * collectors, equivalence is relaxed to allow for non-equality related to
 * differences in order.  (For example, an unordered collector that accumulated
 * elements to a {@code List} would consider two lists equivalent if they
 * contained the same elements, ignoring order.)
 *
 * <p>Libraries that implement reduction based on {@code Collector}, such as
 * {@link Stream#collect(Collector)}, must adhere to the following constraints:
 * <ul>
 *     <li>The first argument passed to the accumulator function, both
 *     arguments passed to the combiner function, and the argument passed to the
 *     finisher function must be the result of a previous invocation of the
 *     result supplier, accumulator, or combiner functions.</li>
 *     <li>The implementation should not do anything with the result of any of
 *     the result supplier, accumulator, or combiner functions other than to
 *     pass them again to the accumulator, combiner, or finisher functions,
 *     or return them to the caller of the reduction operation.</li>
 *     <li>If a result is passed to the combiner or finisher
 *     function, and the same object is not returned from that function, it is
 *     never used again.</li>
 *     <li>Once a result is passed to the combiner or finisher function, it
 *     is never passed to the accumulator function again.</li>
 *     <li>For non-concurrent collectors, any result returned from the result
 *     supplier, accumulator, or combiner functions must be serially
 *     thread-confined.  This enables collection to occur in parallel without
 *     the {@code Collector} needing to implement any additional synchronization.
 *     The reduction implementation must manage that the input is properly
 *     partitioned, that partitions are processed in isolation, and combining
 *     happens only after accumulation is complete.</li>
 *     <li>For concurrent collectors, an implementation is free to (but not
 *     required to) implement reduction concurrently.  A concurrent reduction
 *     is one where the accumulator function is called concurrently from
 *     multiple threads, using the same concurrently-modifiable result container,
 *     rather than keeping the result isolated during accumulation.
 *     A concurrent reduction should only be applied if the collector has the
 *     {@link Characteristics#UNORDERED} characteristics or if the
 *     originating data is unordered.</li>
 * </ul>
 *
 * <p>In addition to the predefined implementations in {@link Collectors}, the
 * static factory methods {@link #of(Supplier, BiConsumer, BinaryOperator, Characteristics...)}
 * can be used to construct collectors.  For example, you could create a collector
 * that accumulates widgets into a {@code TreeSet} with:
 *
 * <pre>{@code
 *     Collector<Widget, ?, TreeSet<Widget>> intoSet =
 *         Collector.of(TreeSet::new, TreeSet::add,
 *                      (left, right) -> { left.addAll(right); return left; });
 * }</pre>
 * <p>
 * (This behavior is also implemented by the predefined collector
 * {@link Collectors#toCollection(Supplier)}).
 *
 * @param <T> the type of input elements to the reduction operation
 * @param <A> the mutable accumulation type of the reduction operation (often
 *            hidden as an implementation detail)
 * @param <R> the result type of the reduction operation
 * @apiNote Performing a reduction operation with a {@code Collector} should produce a
 * result equivalent to:
 * <pre>{@code
 *     R container = collector.supplier().get();
 *     for (T t : data)
 *         collector.accumulator().accept(container, t);
 *     return collector.finisher().apply(container);
 * }</pre>
 *
 * <p>However, the library is free to partition the input, perform the reduction
 * on the partitions, and then use the combiner function to combine the partial
 * results to achieve a parallel reduction.  (Depending on the specific reduction
 * operation, this may perform better or worse, depending on the relative cost
 * of the accumulator and combiner functions.)
 *
 * <p>Collectors are designed to be <em>composed</em>; many of the methods
 * in {@link Collectors} are functions that take a collector and produce
 * a new collector.  For example, given the following collector that computes
 * the sum of the salaries of a stream of employees:
 *
 * <pre>{@code
 *     Collector<Employee, ?, Integer> summingSalaries
 *         = Collectors.summingInt(Employee::getSalary))
 * }</pre>
 * <p>
 * If we wanted to create a collector to tabulate the sum of salaries by
 * department, we could reuse the "sum of salaries" logic using
 * {@link Collectors#groupingBy(Function, Collector)}:
 *
 * <pre>{@code
 *     Collector<Employee, ?, Map<Department, Integer>> summingSalariesByDept
 *         = Collectors.groupingBy(Employee::getDepartment, summingSalaries);
 * }</pre>
 * <p>
 * 一种可变的约简操作，它将输入元素累积到一个可变的结果容器中，可选地将累积的结果转换为所有输入元素处理后的最终表示形式。
 * 约简操作可以顺序执行，也可以并行执行。可变约简操作的例子包括:
 * 将元素累积到集合中;使用StringBuilder连接字符串;计算有关元素(如sum、min、max或average)的汇总信息;计算“数据透视表”，如“卖方最大价值交易”等摘要。
 * 类收集器提供了许多常见的可变约简的实现。收集器由四个函数指定，它们共同将条目累积到可变的结果容器中，并可选地对结果执行最终转换。它们是:
 * * 创建新的结果容器(supplier())，
 * * 将新的数据元素合并到结果容器(accumulator())中，
 * * 将两个结果容器合并到一个结果容器(combiner())中，
 * * 对容器执行可选的最终转换(finisher())
 * <p>
 * 收集器还具有一组特征，如Collector.Characteristics。并发，它提供了一些提示，可以被reduce实现用于提供更好的性能。
 * 使用收集器进行约简的顺序实现将使用supplier函数创建单个结果容器，并为每个输入元素调用accumulator函数一次。
 * 并行实现将对输入进行分区，为每个分区创建结果容器，将每个分区的内容累积到该分区的子结果中，然后使用组合器函数将子结果合并到组合结果中。
 * 为了确保顺序执行和并行执行产生相等的结果，收集器函数必须满足标识和关联约束。
 * identity约束表示，对于任何部分累积的结果，将其与空结果容器组合必须产生等效的结果。
 * 也就是说，对于任何系列累加器和组合器调用的部分累积结果a，必须与组合器等效。应用(supplier.get ())。
 * 根据结合律，分割计算必须得到一个等价的结果。也就是说，对于任意输入元素t1和t2，下面的计算结果r1和r2必须相等:
 * <pre>{@code
 *      A a1 = supplier.get();
 *      accumulator.accept(a1, t1);
 *      accumulator.accept(a1, t2);
 *      R r1 = finisher.apply(a1);  // result without splitting
 *
 *      A a2 = supplier.get();
 *      accumulator.accept(a2, t1);
 *      A a3 = supplier.get();
 *      accumulator.accept(a3, t2);
 *      R r2 = finisher.apply(combiner.apply(a2, a3));  // result with splitting
 * }</pre>
 * <p>
 * 对于没有无序特性的收集器，如果fincher .apply(a1).equals(fincher .apply(a2))，则两个累积结果a1和a2是相等的。
 * 对于无序收集器，可以放宽等价性，以允许与顺序差异相关的非相等性。
 * (例如，一个无序的收集器将元素累积到一个列表中，如果两个列表包含相同的元素，则认为它们是等价的，而忽略了顺序。)
 * 库,实现减少基于收集器,比如Stream.collect(收集器),必须坚持以下约束:
 * 第一个参数传递给累加器功能,两种观点都传递到组合器函数,和修整器的参数传递给函数必须的结果之前的调用结果的供应商,累加器,或组合器功能。
 * 实现不应该对结果提供者、累加器或组合器函数的结果做任何事情，除了再次将它们传递给累加器、组合器或结束器函数，或者将它们返回给缩减操作的调用者。
 * 如果一个结果被传递给组合器或终结器函数，而该函数没有返回相同的对象，则该对象将永远不再使用。
 * 一旦一个结果被传递给组合器或结束器函数，它就再也不会被传递给累加器函数。
 * 对于非并发收集器，从结果提供者、累加器或组合器函数返回的任何结果都必须是串行线程限制的。
 * 这使得收集可以并行进行，而收集器不需要实现任何额外的同步。reduce实现必须管理正确分区的输入、隔离处理的分区以及仅在累积完成后才进行组合。
 * 对于并发收集器，实现可以(但不需要)并发实现reduce。
 * 并发约简是指从多个线程并发地调用accumulator函数，使用相同的并发可修改的结果容器，而不是在累积期间将结果隔离。
 * 只有当收集器具有collector .特性时，才应该应用并发缩减。无序特征或原始数据是无序的。
 * 除了在收集器中预定义的实现外，还可以使用(Supplier、BiConsumer、BinaryOperator、Characteristics…)的静态工厂方法构造收集器。
 * 例如，你可以创建一个收集器，将小部件积累到一个树集:
 * <pre>{@code
 * Collector<Widget, ?, TreeSet<Widget>> intoSet =
 *          Collector.of(TreeSet::new, TreeSet::add,
 *                       (left, right) -> { left.addAll(right); return left; });
 * }</pre>
 * (此行为也由预定义的collector collections . tocollection (Supplier)实现)。
 * @see Stream#collect(Collector)
 * @see Collectors
 * @since 1.8
 */
public interface Collector<T, A, R> {
    /**
     * A function that creates and returns a new mutable result container.
     * 一个创建并返回一个新的可变结果容器的函数。
     *
     * @return a function which returns a new, mutable result container
     */
    Supplier<A> supplier();

    /**
     * A function that folds a value into a mutable result container.
     * 将值折叠到可变结果容器中的函数。
     *
     * @return a function which folds a value into a mutable result container
     */
    BiConsumer<A, T> accumulator();

    /**
     * A function that accepts two partial results and merges them.  The
     * combiner function may fold state from one argument into the other and
     * return that, or may return a new result container.
     * 一个接受两个部分结果并将它们合并的函数。组合器函数可以将状态从一个参数折叠到另一个参数并返回该参数，或者返回一个新的结果容器。
     *
     * @return a function which combines two partial results into a combined
     * result
     */
    BinaryOperator<A> combiner();

    /**
     * Perform the final transformation from the intermediate accumulation type
     * {@code A} to the final result type {@code R}.
     *
     * <p>If the characteristic {@code IDENTITY_FINISH} is
     * set, this function may be presumed to be an identity transform with an
     * unchecked cast from {@code A} to {@code R}.
     * 执行从中间累加类型A到最终结果类型R的最终转换。
     *
     * @return a function which transforms the intermediate result to the final
     * result
     */
    Function<A, R> finisher();

    /**
     * Returns a {@code Set} of {@code Collector.Characteristics} indicating
     * the characteristics of this Collector.  This set should be immutable.
     * 返回一组收集器。指示此收集器特性的特性。这个集合应该是不可变的。
     *
     * @return an immutable set of collector characteristics
     */
    Set<Characteristics> characteristics();

    /**
     * Returns a new {@code Collector} described by the given {@code supplier},
     * {@code accumulator}, and {@code combiner} functions.  The resulting
     * {@code Collector} has the {@code Collector.Characteristics.IDENTITY_FINISH}
     * characteristic.
     * 返回由给定的供应商、累加器和组合器函数描述的新收集器。产生的收集器具有收集器的特征。IDENTITY_FINISH特点。
     *
     * @param supplier        The supplier function for the new collector
     * @param accumulator     The accumulator function for the new collector
     * @param combiner        The combiner function for the new collector
     * @param characteristics The collector characteristics for the new
     *                        collector
     * @param <T>             The type of input elements for the new collector
     * @param <R>             The type of intermediate accumulation result, and final result,
     *                        for the new collector
     * @return the new {@code Collector}
     * @throws NullPointerException if any argument is null
     */
    public static <T, R> Collector<T, R, R> of(Supplier<R> supplier,
                                               BiConsumer<R, T> accumulator,
                                               BinaryOperator<R> combiner,
                                               Characteristics... characteristics) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        Objects.requireNonNull(characteristics);
        Set<Characteristics> cs = (characteristics.length == 0)
                ? Collectors.CH_ID
                : Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH,
                characteristics));
        return new Collectors.CollectorImpl<>(supplier, accumulator, combiner, cs);
    }

    /**
     * Returns a new {@code Collector} described by the given {@code supplier},
     * {@code accumulator}, {@code combiner}, and {@code finisher} functions.
     * 返回由给定的供应商、累加器、组合器和完成器函数描述的新收集器。
     *
     * @param supplier        The supplier function for the new collector
     * @param accumulator     The accumulator function for the new collector
     * @param combiner        The combiner function for the new collector
     * @param finisher        The finisher function for the new collector
     * @param characteristics The collector characteristics for the new
     *                        collector
     * @param <T>             The type of input elements for the new collector
     * @param <A>             The intermediate accumulation type of the new collector
     * @param <R>             The final result type of the new collector
     * @return the new {@code Collector}
     * @throws NullPointerException if any argument is null
     */
    public static <T, A, R> Collector<T, A, R> of(Supplier<A> supplier,
                                                  BiConsumer<A, T> accumulator,
                                                  BinaryOperator<A> combiner,
                                                  Function<A, R> finisher,
                                                  Characteristics... characteristics) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        Objects.requireNonNull(finisher);
        Objects.requireNonNull(characteristics);
        Set<Characteristics> cs = Collectors.CH_NOID;
        if (characteristics.length > 0) {
            cs = EnumSet.noneOf(Characteristics.class);
            Collections.addAll(cs, characteristics);
            cs = Collections.unmodifiableSet(cs);
        }
        return new Collectors.CollectorImpl<>(supplier, accumulator, combiner, finisher, cs);
    }

    /**
     * Characteristics indicating properties of a {@code Collector}, which can
     * be used to optimize reduction implementations.
     * 指示收集器属性的特征，可用于优化约简实现。
     */
    enum Characteristics {
        /**
         * Indicates that this collector is <em>concurrent</em>, meaning that
         * the result container can support the accumulator function being
         * called concurrently with the same result container from multiple
         * threads.
         *
         * <p>If a {@code CONCURRENT} collector is not also {@code UNORDERED},
         * then it should only be evaluated concurrently if applied to an
         * unordered data source.
         * 指示此收集器是并发的，这意味着结果容器可以支持与来自多个线程的相同结果容器并发调用的accumulator函数。
         * 如果并发收集器也不是无序的，那么只有应用到无序数据源时才应该并发地计算它。
         */
        CONCURRENT,

        /**
         * Indicates that the collection operation does not commit to preserving
         * the encounter order of input elements.  (This might be true if the
         * result container has no intrinsic order, such as a {@link Set}.)
         * 指示集合操作未提交保存输入元素的相遇顺序。(如果结果容器没有诸如集合之类的内在顺序，则可能是这样。)
         */
        UNORDERED,

        /**
         * Indicates that the finisher function is the identity function and
         * can be elided.  If set, it must be the case that an unchecked cast
         * from A to R will succeed.
         * 表示完成函数是恒等函数，可以省略。如果设置，则从A到R的未检查强制转换必须成功。
         */
        IDENTITY_FINISH
    }
}
