package com.Auroral.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.Auroral.blog.dao.ArticleDao;
import com.Auroral.blog.dto.CategoryDTO;
import com.Auroral.blog.dto.PageDTO;
import com.Auroral.blog.entity.Article;
import com.Auroral.blog.entity.Category;
import com.Auroral.blog.dao.CategoryDao;
import com.Auroral.blog.exception.ServeException;
import com.Auroral.blog.service.CategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.Auroral.blog.vo.CategoryVO;
import com.Auroral.blog.vo.ConditionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * @author Auroral
 * @since 2020-05-18
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, Category> implements CategoryService {
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private ArticleDao articleDao;

    @Override
    public PageDTO<CategoryDTO> listCategories() {
        // 获取所有分类,包括没有文章的分类
        List<Category> allCategories = categoryDao.selectList(null);

        // 查询每个分类下的文章数量
        List<CategoryDTO> categoryDTOList = allCategories.stream().map(category -> {
            CategoryDTO dto = new CategoryDTO();
            dto.setId(category.getId());
            dto.setCategoryName(category.getCategoryName());

            // 查询该分类下的非删除、非草稿文章数量
            Integer articleCount = articleDao.selectCount(new LambdaQueryWrapper<Article>()
                    .eq(Article::getCategoryId, category.getId())
                    .eq(Article::getIsDelete, 0)  // 添加：只统计未删除的文章
                    .eq(Article::getIsDraft, 0)); // 添加：只统计非草稿的文章

            dto.setArticleCount(articleCount);
            return dto;
        }).collect(Collectors.toList());

        return new PageDTO<>(categoryDTOList, categoryDTOList.size());
    }
    @Override
    public PageDTO<Category> listCategoryBackDTO(ConditionVO condition) {
        // 分页查询分类列表
        Page<Category> page = new Page<>(condition.getCurrent(), condition.getSize());
        Page<Category> categoryPage = categoryDao.selectPage(page, new LambdaQueryWrapper<Category>()
                .select(Category::getId, Category::getCategoryName, Category::getCreateTime)
                .like(StringUtils.isNotBlank(condition.getKeywords()), Category::getCategoryName, condition.getKeywords())
                .orderByDesc(Category::getId));
        return new PageDTO<>(categoryPage.getRecords(), (int) categoryPage.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteCategory(List<Integer> categoryIdList) {
        // 查询分类id下是否有文章
        Integer count = articleDao.selectCount(new LambdaQueryWrapper<Article>()
                .in(Article::getCategoryId, categoryIdList));
        if (count > 0) {
            throw new ServeException("删除失败，该分类下存在文章");
        }
        categoryDao.deleteBatchIds(categoryIdList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateCategory(CategoryVO categoryVO) {
        // 判断分类名重复
        Integer count = categoryDao.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getCategoryName, categoryVO.getCategoryName()));
        if (count > 0) {
            throw new ServeException("分类名已存在");
        }
        Category category = Category.builder()
                .id(categoryVO.getId())
                .categoryName(categoryVO.getCategoryName())
                .createTime(Objects.isNull(categoryVO.getId()) ? new Date() : null)
                .build();
        this.saveOrUpdate(category);
    }

}
