package de.gruenbaum.simpledo.listeners;

import de.gruenbaum.simpledo.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
}
