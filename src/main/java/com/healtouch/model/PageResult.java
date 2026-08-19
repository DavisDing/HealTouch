package com.healtouch.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A single, immutable page of a query result. */
public final class PageResult<T> {
  private final List<T> items;
  private final long totalItems;
  private final int page;
  private final int pageSize;

  public PageResult(List<T> items, long totalItems, int page, int pageSize) {
    if (items == null) {
      throw new IllegalArgumentException("分页数据不能为空");
    }
    if (totalItems < 0 || page < 1 || pageSize < 1) {
      throw new IllegalArgumentException("分页参数不正确");
    }
    this.items = Collections.unmodifiableList(new ArrayList<T>(items));
    this.totalItems = totalItems;
    this.page = page;
    this.pageSize = pageSize;
  }

  public List<T> getItems() {
    return items;
  }

  public long getTotalItems() {
    return totalItems;
  }

  public int getPage() {
    return page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getTotalPages() {
    long pages = Math.max(1L, (totalItems + pageSize - 1L) / pageSize);
    return pages > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pages;
  }

  public boolean hasPreviousPage() {
    return page > 1;
  }

  public boolean hasNextPage() {
    return page < getTotalPages();
  }
}
