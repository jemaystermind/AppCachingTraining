package me.jemaystermind.appcachingtraining

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nytimes.android.external.store3.base.impl.BarCode
import com.nytimes.android.external.store3.base.impl.StoreBuilder
import com.nytimes.android.external.store3.middleware.GsonParserFactory
import com.nytimes.android.external.store3.middleware.GsonTransformerFactory
import com.nytimes.android.external.store3.middleware.moshi.MoshiTransformerFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber
import kotlinx.android.synthetic.main.activity_main.*
import okio.BufferedSource
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gson = Gson()
        val retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .validateEagerly(BuildConfig.DEBUG)
                .build()

        val repo: GithubService = retrofit.create(GithubService::class.java)

        val repoFetcher = { githubService: GithubService, username: String ->
            githubService.reposRaw(username).map { it.source() }
//                        .compose(GsonTransformerFactory.createObjectToSourceTransformer<List<Repos>>(gson))
//                        .compose(MoshiTransformerFactory.createObjectToSourceTransformer(object: TypeToken<List<Repos>>() {}.type))
        }

        val repoParser = GsonParserFactory.createSourceParser<List<Repos>>(gson, object : TypeToken<List<Repos>>() {}.type)

        val reposStore = StoreBuilder.parsedWithKey<BarCode, BufferedSource, List<Repos>>()
                .fetcher { barcode -> repoFetcher(repo, barcode.key) }
                .parser(repoParser)
                .open()

        val jemaystermind = BarCode("repos", "jemaystermind")
        reposStore.get(jemaystermind)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onSuccess = {
                            repos.text = it.toString()
                            Timber.i(it.toString())
                        },
                        onError = {
                            it.printStackTrace()
                            Timber.e(it)
                        }
                )
    }
}
