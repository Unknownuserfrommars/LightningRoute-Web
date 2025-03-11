require('dns').setDefaultResultOrder('ipv4first');

require('dotenv').config();  // Load environment variables
const express = require('express');
const { OpenAI } = require('openai');
const cors = require('cors');

const app = express();
app.use(express.json());
app.use(cors());

const openai = new OpenAI({
    apiKey: process.env.OPENAI_API_KEY
});

// Proxy endpoint: now uses a system message and increased max_tokens
app.post('/api/openai', async (req, res) => {
    try {
        const { prompt } = req.body;
        
        const completion = await openai.chat.completions.create({
            model: "gpt-4",
            messages: [
                { role: "system", content: "You are a mind map generator that converts text into structured mind maps." },
                { role: "user", content: prompt }
            ],
            temperature: 0.7,
            max_tokens: 2000
        });
        
        res.json(completion.choices[0].message.content);
    } catch (error) {
        console.error('OpenAI Error:', error);
        res.status(500).json({ error: 'API Error' });
    }
});

// Serve static files (assumes your static files are in the "public" folder)
app.use(express.static('public'));

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
});
