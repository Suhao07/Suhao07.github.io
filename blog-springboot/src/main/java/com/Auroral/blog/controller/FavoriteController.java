package com.Auroral.blog.controller;

import com.Auroral.blog.annotation.OptLog;
import com.Auroral.blog.constant.StatusConst;
import com.Auroral.blog.dto.FavoriteCategoryDTO;
import com.Auroral.blog.dto.FavoriteDTO;
import com.Auroral.blog.service.FavoriteService;
import com.Auroral.blog.vo.FavoriteCategoryVO;
import com.Auroral.blog.vo.FavoriteVO;
import com.Auroral.blog.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static com.Auroral.blog.constant.OptTypeConst.*;

/**
 * 收藏网站控制器
 *
 * @author Auroral
 */
@Api(tags = "收藏网站模块")
@RestController
@CrossOrigin
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @ApiOperation(value = "查询收藏网站分类列表")
    @GetMapping("/favorites/categories")
    public Result<List<FavoriteCategoryDTO>> listFavoriteCategories() {
        return new Result<>(true, StatusConst.OK, "查询成功", favoriteService.listFavoriteCategories());
    }

    @ApiOperation(value = "查询收藏网站列表")
    @ApiImplicitParam(name = "categoryId", value = "分类ID", dataType = "Integer")
    @GetMapping("/favorites")
    public Result<List<FavoriteDTO>> listFavorites(@RequestParam(required = false) Integer categoryId) {
        return new Result<>(true, StatusConst.OK, "查询成功", favoriteService.listFavorites(categoryId));
    }

    @ApiOperation(value = "根据ID查询收藏网站")
    @ApiImplicitParam(name = "id", value = "收藏网站ID", required = true, dataType = "Integer")
    @GetMapping("/favorites/{id}")
    public Result<FavoriteDTO> getFavoriteById(@PathVariable("id") Integer id) {
        return new Result<>(true, StatusConst.OK, "查询成功", favoriteService.getFavoriteById(id));
    }

    @OptLog(optType = SAVE_OR_UPDATE)
    @ApiOperation(value = "添加或修改收藏网站分类")
    @PostMapping("/admin/favorites/categories")
    public Result<?> saveOrUpdateFavoriteCategory(@Valid @RequestBody FavoriteCategoryVO favoriteCategoryVO) {
        favoriteService.saveOrUpdateFavoriteCategory(favoriteCategoryVO);
        return new Result<>(true, StatusConst.OK, "操作成功");
    }

    @OptLog(optType = SAVE_OR_UPDATE)
    @ApiOperation(value = "添加或修改收藏网站")
    @PostMapping("/admin/favorites")
    public Result<?> saveOrUpdateFavorite(@Valid @RequestBody FavoriteVO favoriteVO) {
        favoriteService.saveOrUpdateFavorite(favoriteVO);
        return new Result<>(true, StatusConst.OK, "操作成功");
    }

    @OptLog(optType = REMOVE)
    @ApiOperation(value = "删除收藏网站分类")
    @ApiImplicitParam(name = "categoryId", value = "分类ID", required = true, dataType = "Integer")
    @DeleteMapping("/admin/favorites/categories/{categoryId}")
    public Result<?> deleteFavoriteCategory(@PathVariable("categoryId") Integer categoryId) {
        favoriteService.deleteFavoriteCategory(categoryId);
        return new Result<>(true, StatusConst.OK, "删除成功");
    }

    @OptLog(optType = REMOVE)
    @ApiOperation(value = "删除收藏网站")
    @ApiImplicitParam(name = "favoriteId", value = "收藏网站ID", required = true, dataType = "Integer")
    @DeleteMapping("/admin/favorites/{favoriteId}")
    public Result<?> deleteFavorite(@PathVariable("favoriteId") Integer favoriteId) {
        favoriteService.deleteFavorite(favoriteId);
        return new Result<>(true, StatusConst.OK, "删除成功");
    }
}