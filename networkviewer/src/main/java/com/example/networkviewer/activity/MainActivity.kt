package com.example.networkviewer.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.networkviewer.R
import com.example.networkviewer.fragment.MainFragment
import com.example.networkviewer.fragment.SplashFragment

class MainActivity : AppCompatActivity(), FragmentedActivity {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        onNextFragment(FragmentType.SPLASH)
    }

    override fun onNextFragment(fragmentType: FragmentType) {
        val fragment = when (fragmentType) {
            FragmentType.SPLASH -> SplashFragment()
            FragmentType.MAIN -> MainFragment()
        }
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
    }
}

enum class FragmentType {
    SPLASH, MAIN
}

interface FragmentedActivity {
    fun onNextFragment(fragmentType: FragmentType)
}