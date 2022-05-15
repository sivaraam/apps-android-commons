package fr.free.nrw.commons.upload

import categoryItem
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoriesModel
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.categories.CategoriesContract
import fr.free.nrw.commons.upload.categories.CategoriesPresenter
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import media
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import java.lang.reflect.Method

/**
 * The class contains unit test cases for CategoriesPresenter
 */
class CategoriesPresenterTest {
    @Mock
    internal lateinit var repository: UploadRepository

    @Mock
    internal lateinit var view: CategoriesContract.View

    private lateinit var categoriesPresenter: CategoriesPresenter

    private lateinit var testScheduler: TestScheduler

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testScheduler = TestScheduler()
        categoriesPresenter = CategoriesPresenter(repository, testScheduler, testScheduler)
        categoriesPresenter.onAttachView(view)
    }

    @Test
    @Throws(Exception::class)
    fun testOnAttachViewWithMedia() {
        categoriesPresenter.onAttachViewWithMedia(view, media())
    }

    @Test
    @Throws(Exception::class)
    fun `Test onAttachViewWithMedia when media is not null`() {
        Whitebox.setInternalState(categoriesPresenter, "media", media())
        whenever(repository.getCategories(repository.selectedExistingCategories))
            .thenReturn(Observable.just(mutableListOf(categoryItem())))
        whenever(repository.searchAll("mock", emptyList(), repository.selectedDepictions))
            .thenReturn(Observable.just(mutableListOf(categoryItem())))
        val method: Method = CategoriesPresenter::class.java.getDeclaredMethod(
            "searchResults",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(categoriesPresenter, "mock")    }

    /**
     * unit test case for method CategoriesPresenter.searchForCategories
     */
    @Test
    fun `searchForCategories combines selection and search results without years distinctly`() {
        val nonEmptyCaptionUploadItem = mock<UploadItem>()
        whenever(nonEmptyCaptionUploadItem.uploadMediaDetails)
            .thenReturn(listOf(UploadMediaDetail(captionText = "nonEmpty")))
        val emptyCaptionUploadItem = mock<UploadItem>()
        whenever(emptyCaptionUploadItem.uploadMediaDetails)
            .thenReturn(listOf(UploadMediaDetail(captionText = "")))
        whenever(repository.uploads).thenReturn(
            listOf(
                nonEmptyCaptionUploadItem,
                emptyCaptionUploadItem
            )
        )
        whenever(repository.searchAll("test", listOf("nonEmpty"), repository.selectedDepictions))
            .thenReturn(
                Observable.just(
                    listOf(
                        categoryItem("selected"),
                        categoryItem("doesContainYear")
                    )
                )
            )
        whenever(repository.containsYear("selected")).thenReturn(false)
        whenever(repository.containsYear("doesContainYear")).thenReturn(true)
        whenever(repository.selectedCategories).thenReturn(listOf(
            categoryItem("selected", "", "",true)))
        categoriesPresenter.searchForCategories("test")
        testScheduler.triggerActions()
        verify(view).showProgress(true)
        verify(view).showError(null)
        verify(view).setCategories(null)
        verify(view).setCategories(listOf(
            categoryItem("selected", "", "", true)))
        verify(view).showProgress(false)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `searchForCategoriesTest sets Error when list is empty`() {
        whenever(repository.uploads).thenReturn(listOf())
        whenever(repository.searchAll(any(), any(), any())).thenReturn(Observable.just(listOf()))
        whenever(repository.selectedCategories).thenReturn(listOf())
        categoriesPresenter.searchForCategories("test")
        testScheduler.triggerActions()
        verify(view).showProgress(true)
        verify(view).showError(null)
        verify(view).setCategories(null)
        verify(view).setCategories(listOf())
        verify(view).showProgress(false)
        verify(view).showError(R.string.no_categories_found)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `check filtering out of categories`() {
        doReturn(Observable.just(
            listOf(
                categoryItem("taken on 2014", "Desc", "Thumb", false),
                categoryItem("test 2014", "Desc", "Thumb", false),
                categoryItem("japan in 660s", "Desc", "Thumb", false)
            )
        ))
        .`when`(repository)
        .searchAll(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()
        )

        val categoriesModel = Mockito.mock(CategoriesModel::class.java)
        `when`(categoriesModel.containsYear(ArgumentMatchers.anyString())).thenCallRealMethod()
        Whitebox.setInternalState(repository, "categoriesModel", categoriesModel)

        repository.searchAll(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()
        )
        .map { it.filterNot { categoryItem -> repository.containsYear(categoryItem.name) } }
        .test()
        .assertValue(listOf(CategoryItem("test 2014", "Desc", "Thumb", false)))
    }

    @Test
    fun `check irrelevant category is filtered out`() {
        val nonEmptyCaptionUploadItem = mock<UploadItem>()
        whenever(nonEmptyCaptionUploadItem.uploadMediaDetails)
            .thenReturn(listOf(UploadMediaDetail(captionText = "nonEmpty")))
        val emptyCaptionUploadItem = mock<UploadItem>()
        whenever(emptyCaptionUploadItem.uploadMediaDetails)
            .thenReturn(listOf(UploadMediaDetail(captionText = "")))
        whenever(repository.uploads).thenReturn(
            listOf(
                nonEmptyCaptionUploadItem,
                emptyCaptionUploadItem
            )
        )
        whenever(repository.searchAll("Photo", listOf("nonEmpty"), repository.selectedDepictions))
            .thenReturn(
                Observable.just(
                    listOf(
                        categoryItem("Photographs taken randomly"),
                        categoryItem("Photographs taken on 2015-11-08")
                    )
                )
            )
        whenever(repository.selectedCategories).thenReturn(listOf(
            categoryItem("Photographs taken randomly", "", "",true)))

        categoriesPresenter.searchForCategories("Photo")
        testScheduler.triggerActions()
        verify(view).showProgress(true)
        verify(view).showError(null)
        verify(view).setCategories(null)
        verify(view).setCategories(listOf(
            categoryItem("Photographs taken randomly", "", "", true)))
        verify(view).showProgress(false)
        verifyNoMoreInteractions(view)
    }
//
//    @Test
//    fun checkIrrelevantCategoryIsFilteredOut2() {
//        val mockResponse = withMockResponse("Category:Media needing categories as of 5 June 2017")
//        whenever(categoryInterface.searchCategories(
//            ArgumentMatchers.anyString(),
//            ArgumentMatchers.anyInt(),
//            ArgumentMatchers.anyInt()
//        ))
//            .thenReturn(Single.just(mockResponse))
//        categoryClient.searchCategories("Media", 10)
//            .test()
//            .assertValues(emptyList())
//    }
//
//    @Test
//    fun checkOldYearCategoriesAreFilteredOut() {
//        val mockResponse = withMockResponse("Category:1970s")
//        whenever(categoryInterface.searchCategories(
//            ArgumentMatchers.anyString(),
//            ArgumentMatchers.anyInt(),
//            ArgumentMatchers.anyInt()
//        ))
//            .thenReturn(Single.just(mockResponse))
//        categoryClient.searchCategories("19", 10)
//            .test()
//            .assertValues(emptyList())
//    }
//
//    @Test
//    fun checkRecentYearCategoriesAreNotFilteredOut() {
//        val mockResponse = withMockResponse("Category:2020s")
//        whenever(categoryInterface.searchCategories(
//            ArgumentMatchers.anyString(),
//            ArgumentMatchers.anyInt(),
//            ArgumentMatchers.anyInt()
//        ))
//            .thenReturn(Single.just(mockResponse))
//        categoryClient.searchCategories("20", 10)
//            .test()
//            .assertValues(listOf(CategoryItem("2020s", "", "", false)))
//    }
//
//    @Test
//    fun checkAllCategoriesWithDateAreNotFilteredOut() {
//        val mockResponse = withMockResponse("Category:Amavenita (ship, 2014)")
//        whenever(categoryInterface.searchCategories(
//            ArgumentMatchers.anyString(),
//            ArgumentMatchers.anyInt(),
//            ArgumentMatchers.anyInt()
//        ))
//            .thenReturn(Single.just(mockResponse))
//        categoryClient.searchCategories("Amav", 10)
//            .test()
//            .assertValues(listOf(CategoryItem("Amavenita (ship, 2014)", "", "", false)))
//    }

    /**
     * unit test for method CategoriesPresenter.verifyCategories
     */
    @Test
    fun `verifyCategories with non empty selection goes to next screen`() {
        val item = categoryItem()
        whenever(repository.selectedCategories).thenReturn(listOf(item))
        categoriesPresenter.verifyCategories()
        verify(repository).setSelectedCategories(listOf(item.name))
        verify(view).goToNextScreen()
    }

    @Test
    fun `verifyCategories with empty selection show no category selected`() {
        whenever(repository.selectedCategories).thenReturn(listOf())
        categoriesPresenter.verifyCategories()
        verify(view).showNoCategorySelected()
    }

    /**
     * Test onCategory Item clicked
     */
    @Test
    fun onCategoryItemClickedTest() {
        val categoryItem = categoryItem()
        categoriesPresenter.onCategoryItemClicked(categoryItem)
        verify(repository).onCategoryClicked(categoryItem, null)
    }

    @Test
    fun testClearPreviousSelection() {
        categoriesPresenter.clearPreviousSelection()
    }

    @Test
    fun testUpdateCategories() {
        categoriesPresenter.updateCategories(media(), "[[Category:Test]]")
    }
}
