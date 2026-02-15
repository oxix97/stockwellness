package org.stockwellness.batch.common;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Processor가 List<T>를 반환할 때, 이를 평탄화(Flatten)하여 실제 Writer에게 전달하는 래퍼 클래스
 */
public class ListItemWriter<T> implements ItemWriter<List<T>> {

    private final ItemWriter<T> delegate;

    public ListItemWriter(ItemWriter<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void write(Chunk<? extends List<T>> chunk) throws Exception {
        List<T> flattenedList = new ArrayList<>();
        
        for (List<T> list : chunk) {
            if (list != null && !list.isEmpty()) {
                flattenedList.addAll(list);
            }
        }

        if (!flattenedList.isEmpty()) {
            delegate.write(new Chunk<>(flattenedList));
        }
    }
}