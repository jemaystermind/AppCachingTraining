package me.jemaystermind.appcachingtraining

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nytimes.android.external.store3.base.impl.BarCode
import com.nytimes.android.external.store3.base.impl.MemoryPolicy
import com.nytimes.android.external.store3.base.impl.Store
import com.nytimes.android.external.store3.base.impl.StoreBuilder
import com.nytimes.android.external.store3.middleware.GsonParserFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import okio.BufferedSource
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    lateinit var reposStore: Store<List<Repos>, BarCode>

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

        reposStore = StoreBuilder.parsedWithKey<BarCode, BufferedSource, List<Repos>>()
                .fetcher { barcode -> repoFetcher(repo, barcode.key) }
                .memoryPolicy(MemoryPolicy.builder()
                        .setExpireAfterTimeUnit(TimeUnit.MINUTES)
                        .setExpireAfterWrite(30)
                        .build())
                .parser(repoParser)
                .open()

        btn_load.setOnClickListener {

            var time = 0L
            reposStore.get(BarCode("repos", "jemaystermind"))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { time = System.currentTimeMillis() }
                    .doAfterTerminate { Timber.i("Duration=${System.currentTimeMillis() - time}")}
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
}
