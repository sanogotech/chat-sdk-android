package sdk.chat.audio;

import android.view.View;

import butterknife.BindView;
import co.chatsdk.message.audio.R;
import co.chatsdk.message.audio.R2;
import co.chatsdk.ui.view_holders.base.BaseOutcomingTextMessageViewHolder;

public class OutcomingAudioMessageViewHolder extends BaseOutcomingTextMessageViewHolder<AudioMessageHolder> {

    @BindView(R2.id.audioPlayerView) protected AudioPlayerView audioPlayerView;

    public OutcomingAudioMessageViewHolder(View itemView, Object payload) {
        super(itemView, payload);
    }

    @Override
    public void onBind(AudioMessageHolder message) {
        super.onBind(message);

        audioPlayerView.buttonColor = R.color.white;
        audioPlayerView.sliderTrackColor = R.color.blue_light;
        audioPlayerView.sliderThumbColor = R.color.white;
        audioPlayerView.textColor = R.color.white;
        audioPlayerView.bind(message.audioURL(), message.getTotalTime());

    }
}