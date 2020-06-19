package com.example.networkviewer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import com.example.networkviewer.R
import com.example.networkviewer.activity.FragmentType
import com.example.networkviewer.activity.FragmentedActivity
import kotlinx.android.synthetic.main.fragment_spalsh.view.*

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_spalsh, container, false)
        view.image.animate()
            .setDuration(1000L)
            .setStartDelay(100L)
            .setInterpolator(DecelerateInterpolator())
            .scaleX(1.7F)
            .scaleY(1.7F)
            .withEndAction { onNotifyParentEndAction() }
            .start()
        return view
    }

    private fun onNotifyParentEndAction() {
        val activity = super.getActivity()
        if (activity is FragmentedActivity) {
            activity.onNextFragment(FragmentType.MAIN)
        }
    }
}
