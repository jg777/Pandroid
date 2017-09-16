package com.jeremiahzucker.pandroid.ui.station

import android.util.Log
import com.jeremiahzucker.pandroid.request.Pandora
import com.jeremiahzucker.pandroid.request.method.exp.user.GetStationList
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * StationListPresenter
 *
 * Author: Jeremiah Zucker
 * Date:   9/2/2017
 * Desc:   TODO: Complete
 */
class StationListPresenter : StationListContract.Presenter {

    private val TAG: String = StationListPresenter::class.java.simpleName
    private var view: StationListContract.View? = null

    override fun attach(view: StationListContract.View) {
        this.view = view
    }

    override fun detach() {
        this.view = null
    }

    override fun getStationList(body: GetStationList.RequestBody) {
        Pandora(Pandora.Protocol.HTTP)
                .RequestBuilder(GetStationList)
                .body(body)
                .build<GetStationList.ResponseBody>()
                .subscribe(this::handleGetStationListSuccess, this::handleGetStationListError)
    }

    private fun handleGetStationListSuccess(responseBody: GetStationList.ResponseBody) {
        if (view == null)
            return

        view?.showProgress(false)
        view?.updateStationList(responseBody.stations)
    }

    private fun handleGetStationListError(throwable: Throwable? = null) {
        // Oh no!
        Log.e(TAG, throwable?.message ?: "Error!", throwable)

        if (view == null)
            return

        view?.showProgress(false)
    }

}