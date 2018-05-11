package com.ivianuu.conductor.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ivianuu.conductor.Conductor
import com.ivianuu.conductor.Router
import com.ivianuu.conductor.RouterTransaction
import com.ivianuu.conductor.sample.controllers.HomeController
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var router: Router

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        router = Conductor.attachRouter(this, controller_container, savedInstanceState)
        if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(HomeController()))
        }
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }

}
