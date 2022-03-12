package de.stevensolleder.simpledo.presenter;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import de.stevensolleder.simpledo.R;
import de.stevensolleder.simpledo.databinding.MainActivityBinding;
import de.stevensolleder.simpledo.model.*;



public class CustomItemTouchHelperCallback extends ItemTouchHelper.Callback
{
    private Main mainActivity;
    private MainActivityBinding mainBinding;
    private RecyclerView.Adapter adapter;
    private ISettingsAccessor settingsAccessor;
    private IDataAccessor dataAccessor;
    private INotificationHelper notificationHelper;
    private EntryViewHolder currentDraggedViewHolder;
    private int distance;

    public CustomItemTouchHelperCallback(Main mainActivity, MainActivityBinding mainBinding, RecyclerView.Adapter adapter, IDataAccessor dataAccessor, ISettingsAccessor settingsAccessor, INotificationHelper notificationHelper)
    {
        this.mainActivity=mainActivity;
        this.mainBinding=mainBinding;
        this.adapter=adapter;
        this.dataAccessor=dataAccessor;
        this.settingsAccessor=settingsAccessor;
        this.notificationHelper=notificationHelper;

        //distance contains how many cards were passed after dropping the card after dragging the card
        this.distance=0;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder)
    {
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
    }

    //onMove() is called when a dragged card is dropped
    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target)
    {
        try
        {
            ((EntryViewHolder)viewHolder).getContextMenu().close();
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
        }

        int fromIndex=viewHolder.getPosition();
        int toIndex=target.getPosition();
        if(fromIndex<toIndex) for (int i=fromIndex; i<toIndex; i++) dataAccessor.swapEntries(i, i+1);
        else for(int i=fromIndex; i>toIndex; i--) dataAccessor.swapEntries(i, i-1);
        adapter.notifyItemMoved(fromIndex, toIndex);

        return viewHolder.getPosition()!=target.getPosition();
    }

    //onMoved() is called when a card is in drag mode and swapped the position with another card
    @Override
    public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromIndex, RecyclerView.ViewHolder target, int toIndex, int x, int y)
    {
        distance+=(fromIndex-toIndex);
        super.onMoved(recyclerView, viewHolder, fromIndex, target, toIndex, x, y);
    }

    //onSwiped() is called when a card is swiped successfully to the left or to the right
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir)
    {
        int position=viewHolder.getPosition();
        Entry entry=dataAccessor.getEntry(position);

        dataAccessor.removeEntry(position);
        adapter.notifyItemRemoved(position);
        if(entry.getDate()!=null) notificationHelper.cancelNotification(entry.getId());

        Snackbar snackbar=Snackbar.make(mainActivity.findViewById(R.id.root), SimpleDo.getAppContext().getResources().getString(R.string.entry_deleted), BaseTransientBottomBar.LENGTH_SHORT);
        snackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);

        if(mainBinding.addCard.getVisibility()== View.VISIBLE) snackbar.setAnchorView(mainBinding.addCard);
        else snackbar.setAnchorView(mainBinding.floatingActionButton);

        snackbar.setAction(SimpleDo.getAppContext().getResources().getString(R.string.undo), (view) ->
        {
            dataAccessor.addEntry(position, entry);
            adapter.notifyItemInserted(position);
            if(entry.getDate()!=null) notificationHelper.planAndSendNotification(entry.getTime(), entry.getDate(), entry.getContent(), entry.getId());
        });

        snackbar.show();
        mainActivity.toggleSortability();
    }

    //onSelectedChanged() is called when the state of the current dragged card changes
    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState)
    {
        if(actionState == ItemTouchHelper.ACTION_STATE_DRAG)
        {
            currentDraggedViewHolder=(EntryViewHolder) viewHolder;
            currentDraggedViewHolder.setEntryDragged(true);
        }

        if(actionState==ItemTouchHelper.ACTION_STATE_IDLE)
        {
            try
            {
                currentDraggedViewHolder.setEntryDragged(false);
            }
            catch(Exception exception)
            {
                exception.printStackTrace();
            }

            if(distance!=0)
            {
                Drawable temp = mainActivity.getResources().getDrawable(R.drawable.ic_swap_vert, mainActivity.getTheme());
                temp.setAlpha(128);
                mainBinding.bottomAppBar.getMenu().getItem(0).setIcon(temp);
                mainBinding.bottomAppBar.getMenu().getItem(0).setEnabled(false);
                settingsAccessor.setSortDirection(Direction.NONE);

                mainBinding.bottomAppBar.getMenu().getItem(1).setIcon(mainActivity.getResources().getDrawable(R.drawable.ic_sort, mainActivity.getTheme()));
                settingsAccessor.setSortCriterion(Criterion.NONE);

                distance=0;
            }
        }
    }
}
