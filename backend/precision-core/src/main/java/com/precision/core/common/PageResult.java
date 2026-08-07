package com.precision.core.common;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应对象
 */
public class PageResult<T> implements Serializable {

    private List<T> list;
    private long total;
    private int pageNum;
    private int pageSize;
    private int pages;

    public PageResult() {}

    public PageResult(List<T> list, long total, int pageNum, int pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
    }

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = pageNum; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public int getPages() { return pages; }
    public void setPages(int pages) { this.pages = pages; }
}
