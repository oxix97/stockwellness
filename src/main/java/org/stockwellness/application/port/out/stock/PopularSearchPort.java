package org.stockwellness.application.port.out.stock;

import java.util.List;

public interface PopularSearchPort {
    void incrementCount(String keyword);
    List<String> findTop10();
}
