package co.chatsdk.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import co.chatsdk.core.dao.Keys;
import co.chatsdk.core.dao.Thread;
import co.chatsdk.core.dao.User;
import co.chatsdk.core.events.EventType;
import co.chatsdk.core.events.NetworkEvent;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.utils.UserListItemConverter;
import co.chatsdk.ui.R;
import co.chatsdk.ui.R2;
import co.chatsdk.ui.adapters.UsersListAdapter;
import co.chatsdk.ui.databinding.FragmentContactsBinding;
import co.chatsdk.ui.utils.ToastHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

public class ThreadUsersFragment extends BaseFragment {

    public class Option {

        @StringRes
        public int resId;
        public Consumer<User> action;

        public Option(int resId, Consumer<User> action) {
            this.resId = resId;
            this.action = action;
        }

        public String getText(@NonNull Activity activity) {
            return activity.getString(resId);
        }
    }

    protected UsersListAdapter adapter;

    protected PublishSubject<User> onClickSubject = PublishSubject.create();
    protected PublishSubject<User> onLongClickSubject = PublishSubject.create();

    protected List<User> sourceUsers = new ArrayList<>();

    protected Thread thread;

    protected AlertDialog userDialog;
    protected AlertDialog rolesDialog;

    protected FragmentContactsBinding b;

    public ThreadUsersFragment(Thread thread) {
        this.thread = thread;
    }

    @Override
    protected @LayoutRes int getLayout() {
        return R.layout.fragment_contacts;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        b = DataBindingUtil.inflate(inflater, getLayout(), container, false);
        rootView = b.getRoot();

        if (thread == null) {
            String threadEntityID = savedInstanceState.getString(Keys.IntentKeyThreadEntityID);
            thread = ChatSDK.db().fetchThreadWithEntityID(threadEntityID);
        }

        initViews();
        addListeners();

        loadData(true);

        return rootView;
    }

    public void addListeners() {
        dm.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterContactsChanged())
                .subscribe(networkEvent -> loadData(true)));

        dm.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.UserPresenceUpdated))
                .subscribe(networkEvent -> loadData(true)));
    }

    public void initViews() {

        // Create the adapter only if null this is here so we wont
        // override the adapter given from the extended class with setAdapter.
        adapter = new UsersListAdapter();

        b.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        b.recyclerView.setAdapter(adapter);

        dm.add(adapter.onClickObservable().subscribe(o -> {
            if (o instanceof User) {
                User user = (User) o;
                onClickSubject.onNext(user);


                List<Option> options = getOptionsForUser(user);

                if (options.size() == 1) {
                    options.get(0).action.accept(user);
                } else {
                    showUserDialog(user);
                }

            }
        }));

        dm.add(adapter.onLongClickObservable().subscribe(o -> {
            if (o instanceof User) {
                User user = (User) o;
                onLongClickSubject.onNext(user);
            }
        }));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (thread != null) {
            outState.putString(Keys.IntentKeyThreadEntityID, thread.getEntityID());
        }
    }

    public void loadData (final boolean force) {
        final ArrayList<User> originalUserList = new ArrayList<>(sourceUsers);
        reloadData();
        if (!originalUserList.equals(sourceUsers) || force) {
            adapter.setUsers(UserListItemConverter.toUserItemList(sourceUsers), true);
        }
    }

    @Override
    public void clearData() {
        if (adapter != null) {
            adapter.clear();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData(true);
    }

    @Override
    public void reloadData() {
        sourceUsers.clear();
        // If this is not a dialog we will load the contacts of the user.
        if (thread != null) {
            // Remove the current user from the list.
            List<User> users = thread.getUsers();
            for (User u : users) {
                if (!u.isMe()) {
                    sourceUsers.add(u);
                }
            }
        }
    }

    protected void showRoleListDialog(User user) {

        if (rolesDialog != null) {
            rolesDialog.dismiss();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.change_role);

        final List<String> roles = ChatSDK.thread().availableRoles(thread, user);
        final String currentRole = ChatSDK.thread().roleForUser(thread, user);
        int checked = roles.indexOf(currentRole);

        builder.setSingleChoiceItems(ChatSDK.thread().availableRoles(thread, user).toArray(new String[0]), checked, (dialog, which) -> {
            String newRole = roles.get(which);
            if (!newRole.equals(currentRole)) {
                dm.add(ChatSDK.thread().setRole(newRole, thread, user).subscribe(() -> {
                    ToastHelper.show(getActivity(), R.string.success);
                }, this));
            }
        });

        rolesDialog = builder.show();
    }

    protected void showUserDialog(User user) {

        if (userDialog != null) {
            userDialog.dismiss();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (getActivity() != null) {

            List<Option> options = getOptionsForUser(user);

            ArrayList<String> optionStrings = new ArrayList<>();
            for (Option o: options) {
                optionStrings.add(o.getText(getActivity()));
            }

            builder.setTitle(getActivity().getString(R.string.actions)).setItems(optionStrings.toArray(new String[0]), (dialog, which) -> {
                try {
                    options.get(which).action.accept(user);
                } catch (Exception e) {
                    ToastHelper.show(getActivity(), e.getLocalizedMessage());
                }
            });
        }
        userDialog = builder.show();
    }

    protected List<Option> getOptionsForUser(User user) {
        ArrayList<Option> options = new ArrayList<>();

        // Add the onClick options
        options.add(new Option(R.string.info, this::showProfile));

        if (ChatSDK.thread().rolesEnabled(thread) && ChatSDK.thread().availableRoles(thread, user).size() > 0) {
            options.add(new Option(R.string.change_role, this::showRoleListDialog));
        }

        if (thread.getCreator().isMe()) {
            options.add(new Option(R.string.remove_from_group, u -> {
                dm.add(ChatSDK.thread().removeUsersFromThread(thread, u).subscribe(() -> {
                    ToastHelper.show(getActivity(), R.string.success);
                }, this));
            }));
        }

        return options;
    }

    protected void showProfile(User user) {
        ChatSDK.ui().startProfileActivity(getContext(), user.getEntityID());
    }

}