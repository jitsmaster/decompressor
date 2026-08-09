package com.tuna.breathwork

import android.content.Intent
import android.service.quicksettings.TileService

/**
 * Quick-settings tile: same one-tap Calm Now path as the widget.
 */
class CalmNowTileService : TileService() {
    override fun onClick() {
        startActivityAndCollapse(CalmNowActivity.intent(this))
        super.onClick()
    }
}
