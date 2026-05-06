import {configureStore} from '@reduxjs/toolkit';

const store = configureStore({
    reducer: {
        // Add your reducers here
    }, 
    middleware: (getDefaultMiddleware) => getDefaultMiddleware() 
})

export default store;