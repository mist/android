package com.bitlove.fetlife.view.adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bitlove.fetlife.FetLifeApplication;
import com.bitlove.fetlife.R;
import com.bitlove.fetlife.model.pojos.fetlife.dbjson.GroupComment;
import com.bitlove.fetlife.model.pojos.fetlife.dbjson.GroupComment_Table;
import com.bitlove.fetlife.model.pojos.fetlife.dbjson.GroupPost;
import com.bitlove.fetlife.model.pojos.fetlife.dbjson.GroupPost_Table;
import com.bitlove.fetlife.model.pojos.fetlife.dbjson.Member;
import com.bitlove.fetlife.util.ColorUtil;
import com.bitlove.fetlife.util.StringUtil;
import com.facebook.drawee.view.SimpleDraweeView;
import com.raizlabs.android.dbflow.sql.language.Select;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GroupMessagesRecyclerAdapter extends RecyclerView.Adapter<GroupMessageViewHolder> {

    private static final String REQUEST_MORE_CLIENT_ID = GroupMessagesRecyclerAdapter.class.getSimpleName() + "%request_more";

    private final GroupMessageClickListener groupMessageClickListener;

    public static final int ITEM_PER_PAGE = 15;

    public interface GroupMessageClickListener {
        void onMemberClick(String memberId);
        void onMessageMetaClicked(String meta);
        void onRequestPageClick(int page);
    }

    private static final float PENDING_ALPHA = 0.5f;

    private GroupPost groupDiscussion;
    private final String groupDiscussionId;
    private final String groupId;

    private List<GroupComment> itemList;
    private int requestedPageCount;

    public GroupMessagesRecyclerAdapter(String groupId, String groupDiscussionId, GroupMessageClickListener groupMessageClickListener) {
        this.groupId = groupId;
        this.groupDiscussionId = groupDiscussionId;
        this.groupMessageClickListener = groupMessageClickListener;
        this.requestedPageCount = 1;
        loadItems();
    }

    public void refresh() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                //TODO: think of possibility of update only specific items instead of the whole list
                loadItems();
                notifyDataSetChanged();
            }
        });
    }

    private void loadItems() {
        //TODO: think of moving to separate thread with specific DB executor
        groupDiscussion = new Select().from(GroupPost.class).where(GroupPost_Table.id.is(groupDiscussionId)).querySingle();
        if (groupDiscussion == null) {
            return;
        }
        itemList = new Select().from(GroupComment.class).where(GroupComment_Table.groupPostId.is(groupDiscussionId)).orderBy(GroupComment_Table.pending,false).orderBy(GroupComment_Table.date,false).orderBy(GroupComment_Table.id,false).limit(requestedPageCount * ITEM_PER_PAGE).queryList();
        if (itemList.size() == requestedPageCount *ITEM_PER_PAGE) {
            GroupComment requestMorePlaceHolder = new GroupComment();
            requestMorePlaceHolder.setBody(groupDiscussion.getBody());
            requestMorePlaceHolder.setClientId(REQUEST_MORE_CLIENT_ID);
            itemList.add(requestMorePlaceHolder);
        }
        GroupComment baseComment = new GroupComment();
        baseComment.setBody(groupDiscussion.getBody());
        baseComment.setAvatarLink(groupDiscussion.getAvatarLink());
        baseComment.setSenderId(groupDiscussion.getMemberId());
        baseComment.setPending(false);
        baseComment.setCreatedAt(groupDiscussion.getCreatedAt());
        baseComment.setGroupId(groupId);
        baseComment.setGroupPostId(groupDiscussionId);
        baseComment.setSenderNickname(groupDiscussion.getNickname());
        baseComment.setId(groupDiscussion.getId());
        baseComment.setClientId(groupDiscussion.getId());
        itemList.add(baseComment);
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    public GroupComment getItem(int position) {
        return itemList.get(position);
    }

    public GroupPost getGroupPost() {
        return groupDiscussion;
    }

    @Override
    public GroupMessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_message, parent, false);
        return new GroupMessageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(GroupMessageViewHolder messageViewHolder, int position) {
        final GroupComment groupMessage = itemList.get(position);

        if (REQUEST_MORE_CLIENT_ID.equals(groupMessage.getClientId())) {
            messageViewHolder.subText.setText("...");
            messageViewHolder.subText.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.TOP);
            messageViewHolder.subText.setTextSize(TypedValue.COMPLEX_UNIT_DIP,42);
            messageViewHolder.subText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (view.hasOnClickListeners()) {
                        groupMessageClickListener.onRequestPageClick(++requestedPageCount);
                        view.setOnClickListener(null);
                    }
                }
            });
            messageViewHolder.messageTextContainer.setVisibility(View.GONE);
            messageViewHolder.messageContainer.setPadding(messageViewHolder.extendedHPadding, 0, messageViewHolder.extendedHPadding, messageViewHolder.vPadding);
            return;
        }

        messageViewHolder.messageTextContainer.setVisibility(View.VISIBLE);

        String messageBody = groupMessage.getBody().trim();
        messageViewHolder.messageText.setText(StringUtil.parseHtml(messageBody));
        messageViewHolder.subText.setText(groupMessage.getSenderNickname() + messageViewHolder.subMessageSeparator + SimpleDateFormat.getDateTimeInstance().format(new Date(groupMessage.getDate())));
        messageViewHolder.subText.setTextSize(TypedValue.COMPLEX_UNIT_DIP,12);
        messageViewHolder.subText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                groupMessageClickListener.onMessageMetaClicked(GroupComment.MENTION_PREFIX + groupMessage.getSenderNickname() + " ");
            }
        });

        boolean myMessage = groupMessage.getSenderId().equals(messageViewHolder.getSelfMessageId());

        if (myMessage) {
            messageViewHolder.subText.setGravity(Gravity.RIGHT);
            messageViewHolder.messageContainer.setGravity(Gravity.RIGHT);
            messageViewHolder.messageContainer.setPadding(messageViewHolder.extendedHPadding, messageViewHolder.vPadding, messageViewHolder.hPadding, messageViewHolder.vPadding);
            messageViewHolder.memberAvatar.setVisibility(View.GONE);
            messageViewHolder.selfAvatar.setVisibility(View.GONE);
            messageViewHolder.selfAvatar.setImageURI(groupMessage.getAvatarLink());
        } else {
            messageViewHolder.subText.setGravity(Gravity.LEFT);
            messageViewHolder.messageContainer.setGravity(Gravity.LEFT);
            messageViewHolder.messageContainer.setPadding(messageViewHolder.hPadding, messageViewHolder.vPadding, messageViewHolder.extendedHPadding, messageViewHolder.vPadding);
            messageViewHolder.selfAvatar.setVisibility(View.GONE);
            messageViewHolder.memberAvatar.setVisibility(View.VISIBLE);
            messageViewHolder.memberAvatar.setImageURI(groupMessage.getAvatarLink());
        }

        messageViewHolder.memberAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (groupMessageClickListener != null) {
                    groupMessageClickListener.onMemberClick(groupMessage.getSenderId());
                }
            }
        });

        if (groupMessage.getPending()) {
            messageViewHolder.messageText.setTextColor(messageViewHolder.primaryTextColor);
            messageViewHolder.messageText.setAlpha(PENDING_ALPHA);
        } else if (groupMessage.getFailed()) {
            messageViewHolder.messageText.setAlpha(1f);
            messageViewHolder.messageText.setTextColor(messageViewHolder.errorTextColor);
        } else {
            messageViewHolder.messageText.setTextColor(messageViewHolder.primaryTextColor);
            messageViewHolder.messageText.setAlpha(1f);
        }
    }

}

class GroupMessageViewHolder extends RecyclerView.ViewHolder {

    private static final int EXTEND_PADDING_MULTIPLIER = 3;

    LinearLayout messageContainer;
    LinearLayout messageTextContainer;
    TextView messageText, subText;
    String subMessageSeparator;
    SimpleDraweeView memberAvatar, selfAvatar;
    int extendedHPadding, extendedVPadding, hPadding, vPadding;
    public String selfMessageId;
    public int primaryTextColor, errorTextColor;

    public GroupMessageViewHolder(View itemView) {
        super(itemView);

        Context context = itemView.getContext();

        subMessageSeparator = context.getResources().getString(R.string.message_sub_separator);

        hPadding = (int) context.getResources().getDimension(R.dimen.listitem_horizontal_margin);
        vPadding = (int) context.getResources().getDimension(R.dimen.listitem_vertical_margin);
        extendedHPadding = EXTEND_PADDING_MULTIPLIER * hPadding;
        extendedVPadding = EXTEND_PADDING_MULTIPLIER * vPadding;

        primaryTextColor = ColorUtil.retrieverColor(context, R.color.text_color_primary);
        errorTextColor = ColorUtil.retrieverColor(context, R.color.text_color_error);

        messageTextContainer = (LinearLayout) itemView.findViewById(R.id.message_text_container);
        messageContainer = (LinearLayout) itemView.findViewById(R.id.message_container);
        messageText = (TextView) itemView.findViewById(R.id.message_text);
        messageText.setMovementMethod(LinkMovementMethod.getInstance());
        subText = (TextView) itemView.findViewById(R.id.message_sub);
        memberAvatar = (SimpleDraweeView) itemView.findViewById(R.id.left_member_image);
        selfAvatar = (SimpleDraweeView) itemView.findViewById(R.id.right_member_image);
    }

    public String getSelfMessageId() {
        if (selfMessageId == null) {
            FetLifeApplication fetLifeApplication = (FetLifeApplication) messageContainer.getContext().getApplicationContext();
            Member currentUser = fetLifeApplication.getUserSessionManager().getCurrentUser();
            if (currentUser != null) {
                selfMessageId = currentUser.getId();
            }
        }
        return selfMessageId;
    }
}